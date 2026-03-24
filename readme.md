// run this command in terminal to use CLI
java -cp "lib\mysql-connector-j-9.6.0.jar;out" app.Main

//For future recompilation (if you change any file), use this exact command 
javac -encoding UTF-8 -cp "lib\mysql-connector-j-9.6.0.jar;src" -d out src\config\DBConnection.java src\model\UserAccount.java src\model\Item.java src\model\InventoryRecord.java src\model\SalesTransaction.java src\model\TransactionItem.java src\model\Bill.java src\model\PriceHistory.java src\model\Report.java src\dao\UserAccountDAO.java src\dao\ItemDAO.java src\dao\InventoryRecordDAO.java src\dao\SalesTransactionDAO.java src\dao\TransactionItemDAO.java src\dao\PriceHistoryDAO.java src\service\AuthService.java src\service\InventoryService.java src\service\TransactionService.java src\service\ReportService.java src\app\Main.java
