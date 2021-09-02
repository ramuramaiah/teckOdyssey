package guice.mockito;

import org.apache.log4j.Logger;

public class JavaLoggerTransactionLog implements TransactionLog {
	static Logger logger = Logger.getLogger(JavaLoggerTransactionLog.class);

	public void logChargeResult(ChargeResult result) {
		String message = result.wasSuccessful() ? "The transaction was successful" : result.getDeclineMessage();
		logger.info(message);
		
	}

	public void logConnectException(UnreachableException ex) {
		String message ="An error occured while processing the transaction due to error: " + ex.getMessage();
		logger.error(message);
	}
}
