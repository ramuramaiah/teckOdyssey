package guice.mockito;

public interface TransactionLog {
	public void logChargeResult(ChargeResult result);
	public void logConnectException(UnreachableException ex);
}
