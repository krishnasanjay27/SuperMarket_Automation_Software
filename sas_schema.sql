CREATE DATABASE IF NOT EXISTS supermarket_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE supermarket_db;

CREATE TABLE IF NOT EXISTS UserAccount (
    userId        VARCHAR(50)   NOT NULL,
    password      VARCHAR(255)  NOT NULL,
    role          ENUM('SalesStaff', 'InventoryStaff', 'Manager') NOT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT pk_UserAccount PRIMARY KEY (userId)
);

CREATE TABLE IF NOT EXISTS Item (
    itemCode      VARCHAR(30)    NOT NULL,
    itemName      VARCHAR(150)   NOT NULL,
    price         DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    costPrice     DECIMAL(10, 2) NOT NULL CHECK (costPrice >= 0),
    reorderLevel  INT            NOT NULL DEFAULT 10 CHECK (reorderLevel >= 0),
    category      VARCHAR(100)   NOT NULL DEFAULT 'General',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_Item PRIMARY KEY (itemCode)
);

CREATE TABLE IF NOT EXISTS InventoryRecord (
    inventoryId   INT            NOT NULL AUTO_INCREMENT,
    itemCode      VARCHAR(30)    NOT NULL,
    stockLevel    INT            NOT NULL DEFAULT 0 CHECK (stockLevel >= 0),
    lastUpdated   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,
    updatedBy     VARCHAR(50)    NULL,

    CONSTRAINT pk_InventoryRecord    PRIMARY KEY (inventoryId),
    CONSTRAINT uq_InventoryItem      UNIQUE (itemCode),
    CONSTRAINT fk_Inventory_Item     FOREIGN KEY (itemCode)
        REFERENCES Item (itemCode)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_Inventory_UpdatedBy FOREIGN KEY (updatedBy)
        REFERENCES UserAccount (userId)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

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

CREATE TABLE IF NOT EXISTS TransactionItem (
    transactionItemId  INT            NOT NULL AUTO_INCREMENT,
    transactionId      VARCHAR(50)    NOT NULL,
    itemCode           VARCHAR(30)    NOT NULL,
    quantity           INT            NOT NULL CHECK (quantity > 0),
    unitPrice          DECIMAL(10, 2) NOT NULL CHECK (unitPrice >= 0),
    lineTotal          DECIMAL(12, 2) NOT NULL,

    CONSTRAINT pk_TransactionItem           PRIMARY KEY (transactionItemId),
    CONSTRAINT uq_TxnItem                   UNIQUE (transactionId, itemCode),
    CONSTRAINT fk_TxnItem_SalesTransaction  FOREIGN KEY (transactionId)
        REFERENCES SalesTransaction (transactionId)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_TxnItem_Item              FOREIGN KEY (itemCode)
        REFERENCES Item (itemCode)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS Bill (
    billId           INT            NOT NULL AUTO_INCREMENT,
    transactionId    VARCHAR(50)    NOT NULL,
    generatedDate    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    totalAmount      DECIMAL(12, 2) NOT NULL CHECK (totalAmount >= 0),

    CONSTRAINT pk_Bill                     PRIMARY KEY (billId),
    CONSTRAINT uq_Bill_SalesTransaction    UNIQUE (transactionId),
    CONSTRAINT fk_Bill_SalesTransaction    FOREIGN KEY (transactionId)
        REFERENCES SalesTransaction (transactionId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS Report (
    reportId        INT          NOT NULL AUTO_INCREMENT,
    generatedDate   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reportType      ENUM('SALES', 'INVENTORY', 'PRICE_CHANGE') NOT NULL,
    dateRangeStart  DATE         NULL,
    dateRangeEnd    DATE         NULL,
    generatedBy     VARCHAR(50)  NOT NULL,

    CONSTRAINT pk_Report            PRIMARY KEY (reportId),
    CONSTRAINT fk_Report_Manager    FOREIGN KEY (generatedBy)
        REFERENCES UserAccount (userId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_DateRange CHECK (
        dateRangeEnd IS NULL OR dateRangeStart IS NULL OR dateRangeEnd >= dateRangeStart
    )
);

CREATE TABLE IF NOT EXISTS PriceHistory (
    priceHistoryId  INT            NOT NULL AUTO_INCREMENT,
    itemCode        VARCHAR(30)    NOT NULL,
    oldPrice        DECIMAL(10, 2) NOT NULL,
    newPrice        DECIMAL(10, 2) NOT NULL,
    changedAt       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changedBy       VARCHAR(50)    NOT NULL,

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

CREATE INDEX idx_SalesTxn_SalesStaff
    ON SalesTransaction (salesStaffId);

CREATE INDEX idx_SalesTxn_Date
    ON SalesTransaction (transactionDate);

CREATE INDEX idx_SalesTxn_Status
    ON SalesTransaction (status);

CREATE INDEX idx_TxnItem_ItemCode
    ON TransactionItem (itemCode);

CREATE INDEX idx_Inventory_Stock
    ON InventoryRecord (stockLevel);

CREATE INDEX idx_Report_Date
    ON Report (generatedDate);

CREATE INDEX idx_PriceHist_Item
    ON PriceHistory (itemCode, changedAt);

CREATE INDEX idx_Item_Category
    ON Item (category);

DELIMITER $$

CREATE TRIGGER trg_UpdateInventoryOnFinalize
AFTER UPDATE ON SalesTransaction
FOR EACH ROW
BEGIN
    IF NEW.status = 'FINALIZED' AND OLD.status = 'ACTIVE' THEN
        UPDATE InventoryRecord ir
        JOIN   TransactionItem ti ON ti.itemCode = ir.itemCode
        SET    ir.stockLevel  = ir.stockLevel - ti.quantity,
               ir.lastUpdated = NOW()
        WHERE  ti.transactionId = NEW.transactionId;
    END IF;
END$$

DELIMITER ;

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

INSERT INTO UserAccount (userId, password, role) VALUES
('MGR001',  '$2a$12$YourHashedPasswordHere1', 'Manager'),
('SALES001', '$2a$12$YourHashedPasswordHere2', 'SalesStaff'),
('SALES002', '$2a$12$YourHashedPasswordHere3', 'SalesStaff'),
('INV001',   '$2a$12$YourHashedPasswordHere4', 'InventoryStaff');

INSERT INTO Item (itemCode, itemName, price, costPrice, reorderLevel, category) VALUES
('ITM001', 'Basmati Rice 5kg',     299.99, 220.00, 20, 'Grains & Rice'),
('ITM002', 'Sunflower Oil 1L',     149.50, 110.00, 15, 'Oils & Fats'),
('ITM003', 'Whole Wheat Bread',     45.00,  28.00, 30, 'Bakery'),
('ITM004', 'Full Cream Milk 1L',    58.00,  40.00, 25, 'Dairy'),
('ITM005', 'Cheddar Cheese 200g',   95.00,  65.00, 10, 'Dairy');

INSERT INTO InventoryRecord (itemCode, stockLevel) VALUES
('ITM001', 150),
('ITM002', 200),
('ITM003', 100),
('ITM004', 180),
('ITM005',  75);

INSERT INTO SalesTransaction (transactionId, salesStaffId, status) VALUES
('TXN20260301001', 'SALES001', 'FINALIZED');

INSERT INTO TransactionItem (transactionId, itemCode, quantity, unitPrice, lineTotal) VALUES
('TXN20260301001', 'ITM001', 2, 299.99,  599.98),
('TXN20260301001', 'ITM003', 3,  45.00,  135.00),
('TXN20260301001', 'ITM004', 1,  58.00,   58.00);

INSERT INTO Bill (transactionId, totalAmount) VALUES
('TXN20260301001', 792.98);

INSERT INTO PriceHistory (itemCode, oldPrice, newPrice, changedBy) VALUES
('ITM002', 139.50, 149.50, 'MGR001');

INSERT INTO Report (reportType, dateRangeStart, dateRangeEnd, generatedBy) VALUES
('SALES', '2026-03-01', '2026-03-22', 'MGR001');

-- ============================================================
-- EXTENSION: Customer Records + Loyalty Points Discount System
-- ============================================================

CREATE TABLE IF NOT EXISTS Customer (
    customerId    INT           NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)  NOT NULL,
    phone         VARCHAR(10)   NOT NULL,
    email         VARCHAR(100)  NULL,
    address       VARCHAR(255)  NULL,
    loyaltyPoints INT           NOT NULL DEFAULT 0,

    CONSTRAINT pk_Customer         PRIMARY KEY (customerId),
    CONSTRAINT uq_Customer_Phone   UNIQUE (phone),
    CONSTRAINT chk_Customer_Phone  CHECK (phone REGEXP '^[0-9]{10}$')
);

CREATE INDEX idx_Customer_Phone ON Customer (phone);

ALTER TABLE SalesTransaction
    ADD COLUMN customerId INT NULL,
    ADD CONSTRAINT fk_SalesTxn_Customer
        FOREIGN KEY (customerId) REFERENCES Customer (customerId)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

ALTER TABLE Bill
    ADD COLUMN loyaltyPointsUsed   INT    NOT NULL DEFAULT 0,
    ADD COLUMN loyaltyDiscount     DOUBLE NOT NULL DEFAULT 0.0,
    ADD COLUMN finalTotal          DOUBLE NOT NULL DEFAULT 0.0,
    ADD COLUMN loyaltyPointsEarned INT    NOT NULL DEFAULT 0;




