import java.util.UUID;

public final class Ids {

    private Ids(){
    }   

    public static String newRunId(){
        return UUID.randomUUID().toString();
    }

}