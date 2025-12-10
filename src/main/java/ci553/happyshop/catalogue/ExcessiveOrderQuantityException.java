package ci553.happyshop.catalogue;

public class ExcessiveOrderQuantityException extends RuntimeException {
    public ExcessiveOrderQuantityException(String message) {
        super(message);
    }
}
