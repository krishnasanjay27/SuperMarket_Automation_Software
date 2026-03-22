-- =============================================================
--  Supermarket Automation System (SAS) – MySQL Database Schema
--  Compatible with JDBC-based Java applications
--  Normalised to 3NF | Referential Integrity Enforced
-- =============================================================
-- PATCH v1.1 (2026-03-22)
--   • Renamed Transaction → SalesTransaction (reserved-keyword fix)
--   • Removed uq_ItemName unique constraint
--   • Added InventoryRecord.updatedBy (audit column)
--   • Added Item.category column

-- ---------------------------------------------------------------
-- 1. DATABASE
-- ---------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS supermarket_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE supermarket_db;

-- ---------------------------------------------------------------
-- 2. TABLE: UserAccount
--    Stores all users: SalesStaff, InventoryStaff, Manager.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS UserAccount (
    userId        VARCHAR(50)   NOT NULL,
    password      VARCHAR(255)  NOT NULL COMMENT 'BCrypt hashed password',
    role          ENUM('SalesStaff', 'InventoryStaff', 'Manager') NOT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT pk_UserAccount PRIMARY KEY (userId)
);

-- ---------------------------------------------------------------
-- 3. TABLE: Item
--    Master catalogue of all products sold in the supermarket.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Item (
    itemCode      VARCHAR(30)    NOT NULL,
    itemName      VARCHAR(150)   NOT NULL,
    price         DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    costPrice     DECIMAL(10, 2) NOT NULL CHECK (costPrice >= 0),
    reorderLevel  INT            NOT NULL DEFAULT 10 CHECK (reorderLevel >= 0),
    category      VARCHAR(100)   NOT NULL DEFAULT 'General',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_Item PRIMARY KEY (itemCode)
    -- uq_ItemName removed: multiple items may share the same name
);

-- ---------------------------------------------------------------
-- 4. TABLE: InventoryRecord
--    One-to-one with Item. Tracks current stock level.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS InventoryRecord (
    inventoryId   INT            NOT NULL AUTO_INCREMENT,
    itemCode      VARCHAR(30)    NOT NULL,
    stockLevel    INT            NOT NULL DEFAULT 0 CHECK (stockLevel >= 0),
    lastUpdated   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,
    updatedBy     VARCHAR(50)    NULL COMMENT 'UserId of staff who last updated stock',

    CONSTRAINT pk_InventoryRecord    PRIMARY KEY (inventoryId),
    CONSTRAINT uq_InventoryItem      UNIQUE (itemCode),           -- 1-to-1 with Item
    CONSTRAINT fk_Inventory_Item     FOREIGN KEY (itemCode)
        REFERENCES Item (itemCode)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_Inventory_UpdatedBy FOREIGN KEY (updatedBy)
        REFERENCES UserAccount (userId)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- ---------------------------------------------------------------
-- 5. TABLE: SalesTransaction  (renamed from Transaction to avoid reserved keyword)
--    A sales session initiated by a SalesStaff member.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS SalesTransaction (
    transactionId   VARCHAR(50)    NOT NULL,
    transactionDate DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    totalAmount     DECIMAL(12, 2) NOT NULL DEFAULT 0.00 CHECK (totalAmount >= 0),
    status          ENUM('ACTIVE', 'FINALIZED', 'ABORTED') NOT NULL DEFAULT 'ACTIVE',
    salesStaffId    VARCHAR(50)    NOT NULL,

    CONSTRAINT pk_SalesTransaction       PRIMARY KEY (transactionId),
    CONSTRAINT fk_SalesTransaction_User  FOREIGN KEY (salesStaffId)
        REFERENCES UserAccount (userId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ---------------------------------------------------------------
-- 6. TABLE: TransactionItem
--    Line items (products) belonging to a SalesTransaction.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS TransactionItem (
    transactionItemId  INT            NOT NULL AUTO_INCREMENT,
    transactionId      VARCHAR(50)    NOT NULL,
    itemCode           VARCHAR(30)    NOT NULL,
    quantity           INT            NOT NULL CHECK (quantity > 0),
    unitPrice          DECIMAL(10, 2) NOT NULL CHECK (unitPrice >= 0),
    lineTotal          DECIMAL(12, 2) NOT NULL
        COMMENT 'Stored as quantity * unitPrice for audit trail',

    CONSTRAINT pk_TransactionItem           PRIMARY KEY (transactionItemId),
    CONSTRAINT uq_TxnItem                   UNIQUE (transactionId, itemCode),  -- one row per item per txn
    CONSTRAINT fk_TxnItem_SalesTransaction  FOREIGN KEY (transactionId)
        REFERENCES SalesTransaction (transactionId)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_TxnItem_Item              FOREIGN KEY (itemCode)
        REFERENCES Item (itemCode)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ---------------------------------------------------------------
-- 7. TABLE: Bill
--    Immutable receipt generated when a SalesTransaction is FINALIZED.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Bill (
    billId           INT            NOT NULL AUTO_INCREMENT,
    transactionId    VARCHAR(50)    NOT NULL,
    generatedDate    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    totalAmount      DECIMAL(12, 2) NOT NULL CHECK (totalAmount >= 0),

    CONSTRAINT pk_Bill                     PRIMARY KEY (billId),
    CONSTRAINT uq_Bill_SalesTransaction    UNIQUE (transactionId),      -- one bill per transaction
    CONSTRAINT fk_Bill_SalesTransaction    FOREIGN KEY (transactionId)
        REFERENCES SalesTransaction (transactionId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ---------------------------------------------------------------
-- 8. TABLE: Report
--    Audit record of every report generated by a Manager.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Report (
    reportId        INT          NOT NULL AUTO_INCREMENT,
    generatedDate   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reportType      ENUM('SALES', 'INVENTORY', 'PRICE_CHANGE') NOT NULL,
    dateRangeStart  DATE         NULL COMMENT 'Inclusive start date for date-range reports',
    dateRangeEnd    DATE         NULL COMMENT 'Inclusive end date for date-range reports',
    generatedBy     VARCHAR(50)  NOT NULL COMMENT 'Manager userId who requested the report',

    CONSTRAINT pk_Report            PRIMARY KEY (reportId),
    CONSTRAINT fk_Report_Manager    FOREIGN KEY (generatedBy)
        REFERENCES UserAccount (userId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_DateRange CHECK (
        dateRangeEnd IS NULL OR dateRangeStart IS NULL OR dateRangeEnd >= dateRangeStart
    )
);

-- ---------------------------------------------------------------
-- 9. TABLE: PriceHistory  (audit trail for price modifications)
--    Keeps an immutable record every time a Manager changes a price.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS PriceHistory (
    priceHistoryId  INT            NOT NULL AUTO_INCREMENT,
    itemCode        VARCHAR(30)    NOT NULL,
    oldPrice        DECIMAL(10, 2) NOT NULL,
    newPrice        DECIMAL(10, 2) NOT NULL,
    changedAt       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changedBy       VARCHAR(50)    NOT NULL COMMENT 'Manager userId',

    CONSTRAINT pk_PriceHistory       PRIMARY KEY (priceHistoryId),
    CONSTRAINT fk_PriceHist_Item     FOREIGN KEY (itemCode)
        REFERENCES Item (itemCode)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_PriceHist_Manager  FOREIGN KEY (changedBy)
        REFERENCES UserAccount (userId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ---------------------------------------------------------------
-- 10. INDEXES (performance on hot-path queries)
-- ---------------------------------------------------------------

-- SalesTransactions looked up by staff member (e.g., history per cashier)
CREATE INDEX idx_SalesTxn_SalesStaff
    ON SalesTransaction (salesStaffId);

-- SalesTransactions filtered by date (reporting)
CREATE INDEX idx_SalesTxn_Date
    ON SalesTransaction (transactionDate);

-- SalesTransaction status filter (find ACTIVE transactions quickly)
CREATE INDEX idx_SalesTxn_Status
    ON SalesTransaction (status);

-- TransactionItem lookups by item (sales volume per item)
CREATE INDEX idx_TxnItem_ItemCode
    ON TransactionItem (itemCode);

-- InventoryRecord low-stock queries
CREATE INDEX idx_Inventory_Stock
    ON InventoryRecord (stockLevel);

-- Report date lookups
CREATE INDEX idx_Report_Date
    ON Report (generatedDate);

-- Price history per item (chronological audit)
CREATE INDEX idx_PriceHist_Item
    ON PriceHistory (itemCode, changedAt);

-- Item category lookups (category-wise reporting)
CREATE INDEX idx_Item_Category
    ON Item (category);

-- ---------------------------------------------------------------
-- 11. TRIGGER: Auto-decrement stock when a SalesTransaction is FINALIZED
-- ---------------------------------------------------------------
DELIMITER $$

CREATE TRIGGER trg_UpdateInventoryOnFinalize
AFTER UPDATE ON SalesTransaction
FOR EACH ROW
BEGIN
    IF NEW.status = 'FINALIZED' AND OLD.status = 'ACTIVE' THEN
        -- Decrement stock for every line item in this transaction
        UPDATE InventoryRecord ir
        JOIN   TransactionItem ti ON ti.itemCode = ir.itemCode
        SET    ir.stockLevel  = ir.stockLevel - ti.quantity,
               ir.lastUpdated = NOW()
        WHERE  ti.transactionId = NEW.transactionId;
    END IF;
END$$

DELIMITER ;

-- ---------------------------------------------------------------
-- 12. TRIGGER: Keep SalesTransaction.totalAmount in sync with line items
-- ---------------------------------------------------------------
DELIMITER $$

CREATE TRIGGER trg_RecalcTotalOnInsert
AFTER INSERT ON TransactionItem
FOR EACH ROW
BEGIN
    UPDATE SalesTransaction
    SET    totalAmount = (
               SELECT COALESCE(SUM(lineTotal), 0)
               FROM   TransactionItem
               WHERE  transactionId = NEW.transactionId
           )
    WHERE  transactionId = NEW.transactionId;
END$$

CREATE TRIGGER trg_RecalcTotalOnUpdate
AFTER UPDATE ON TransactionItem
FOR EACH ROW
BEGIN
    UPDATE SalesTransaction
    SET    totalAmount = (
               SELECT COALESCE(SUM(lineTotal), 0)
               FROM   TransactionItem
               WHERE  transactionId = NEW.transactionId
           )
    WHERE  transactionId = NEW.transactionId;
END$$

CREATE TRIGGER trg_RecalcTotalOnDelete
AFTER DELETE ON TransactionItem
FOR EACH ROW
BEGIN
    UPDATE SalesTransaction
    SET    totalAmount = (
               SELECT COALESCE(SUM(lineTotal), 0)
               FROM   TransactionItem
               WHERE  transactionId = OLD.transactionId
           )
    WHERE  transactionId = OLD.transactionId;
END$$

DELIMITER ;

-- ---------------------------------------------------------------
-- 13. SAMPLE DATA (for development / testing)
-- ---------------------------------------------------------------

-- Users (passwords are BCrypt of: Manager@123 / Staff@123)
INSERT INTO UserAccount (userId, password, role) VALUES
('MGR001',  '$2a$12$YourHashedPasswordHere1', 'Manager'),
('SALES001', '$2a$12$YourHashedPasswordHere2', 'SalesStaff'),
('SALES002', '$2a$12$YourHashedPasswordHere3', 'SalesStaff'),
('INV001',   '$2a$12$YourHashedPasswordHere4', 'InventoryStaff');

-- Items (with category)
INSERT INTO Item (itemCode, itemName, price, costPrice, reorderLevel, category) VALUES
('ITM001', 'Basmati Rice 5kg',     299.99, 220.00, 20, 'Grains & Rice'),
('ITM002', 'Sunflower Oil 1L',     149.50, 110.00, 15, 'Oils & Fats'),
('ITM003', 'Whole Wheat Bread',     45.00,  28.00, 30, 'Bakery'),
('ITM004', 'Full Cream Milk 1L',    58.00,  40.00, 25, 'Dairy'),
('ITM005', 'Cheddar Cheese 200g',   95.00,  65.00, 10, 'Dairy');

-- Inventory (initial stock)
INSERT INTO InventoryRecord (itemCode, stockLevel) VALUES
('ITM001', 150),
('ITM002', 200),
('ITM003', 100),
('ITM004', 180),
('ITM005',  75);

-- A sample FINALIZED transaction
INSERT INTO SalesTransaction (transactionId, salesStaffId, status) VALUES
('TXN20260301001', 'SALES001', 'FINALIZED');

INSERT INTO TransactionItem (transactionId, itemCode, quantity, unitPrice, lineTotal) VALUES
('TXN20260301001', 'ITM001', 2, 299.99,  599.98),
('TXN20260301001', 'ITM003', 3,  45.00,  135.00),
('TXN20260301001', 'ITM004', 1,  58.00,   58.00);

-- Bill for the finalized transaction
INSERT INTO Bill (transactionId, totalAmount) VALUES
('TXN20260301001', 792.98);

-- Price-change audit example
INSERT INTO PriceHistory (itemCode, oldPrice, newPrice, changedBy) VALUES
('ITM002', 139.50, 149.50, 'MGR001');

-- Report record
INSERT INTO Report (reportType, dateRangeStart, dateRangeEnd, generatedBy) VALUES
('SALES', '2026-03-01', '2026-03-22', 'MGR001');
