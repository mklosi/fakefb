package fakefb;

import com.fasterxml.jackson.databind.ObjectMapper;
import fakefb.model.Friend;
import fakefb.model.FriendEvent;
import fakefb.model.FriendList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Path("")
public class Resource {

    private static String CHECKPOINT_LOCATION = "/Users/roise0r/fakefb/src/resources/lastProcessedMillisecond";
    private static String MAP_LOCATION = "/Users/roise0r/fakefb/src/resources/map";

    public static Map<String, Set<Friend>> MAP;

    static {
        init();
    }

    private static void init() {

        Long lastProcessedMillisecond = 0L;

        // read state from disk
        try {
            // read lastProcessedMillisecond
            FileInputStream fin = new FileInputStream(CHECKPOINT_LOCATION);
            ObjectInputStream ois = new ObjectInputStream(fin);
            lastProcessedMillisecond = (Long) ois.readObject();
            ois.close();

            // read map
            fin = new FileInputStream(MAP_LOCATION);
            ois = new ObjectInputStream(fin);
            //noinspection unchecked
            MAP = (Map<String, Set<Friend>>) ois.readObject();
            ois.close();
        } catch (IOException|ClassNotFoundException e) {
            lastProcessedMillisecond = 0L;
            MAP = new ConcurrentHashMap<>();
        }

        // &&& maybe add test cases for the whole thing?

        ObjectMapper mapper = new ObjectMapper();
        HttpURLConnection conn = null;
        BufferedReader br = null;

        try { // &&& maybe design these trys better. brake them up.

            // using the actual api
            // throws MalformedURLException
            URL url = new URL("https://immense-river-17982.herokuapp.com/?since=" + String.valueOf(
                    lastProcessedMillisecond));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String jsonStr;

//                for (int i = 0; i < TestData.jsonTestData.length; i++) {
//                    jsonStr = TestData.jsonTestData[i];
            for (int i = 0; ; i++) {

                while ((jsonStr = br.readLine()) == null) {
                    // &&& does it read lines again once it is available? assuming yes
                    // sleep for 2s each time we run out of raw events
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                FriendEvent event = mapper.readValue(jsonStr, FriendEvent.class);

                // from processing
                Set<Friend> fromFriendIds = MAP.get(event.getFrom().getId());
                if (fromFriendIds == null) {
                    fromFriendIds = ConcurrentHashMap.newKeySet();
                    //&&& maybe no need for concurrency here?
                    MAP.put(event.getFrom().getId(), fromFriendIds);
                }

                // to processing
                Set<Friend> toFriendIds = MAP.get(event.getTo().getId());
                if (toFriendIds == null) {
                    toFriendIds = ConcurrentHashMap.newKeySet();
                    MAP.put(event.getTo().getId(), toFriendIds);
                }

                if (event.isAreFriends()) {
                    fromFriendIds.add(event.getTo());
                    toFriendIds.add(event.getFrom());
                } else {
                    fromFriendIds.remove(event.getTo());
                    toFriendIds.remove(event.getFrom());
                }

//                System.out.println("MAP: " + MAP.toString());
                lastProcessedMillisecond = event.getTimestamp();

                // write state to disk every 100 events
                if ((i + 1) % 100 == 0) {
                    try {
                        // write lastProcessedMillisecond
                        FileOutputStream fout = new FileOutputStream(CHECKPOINT_LOCATION);
                        ObjectOutputStream oos = new ObjectOutputStream(fout);
                        oos.writeObject(lastProcessedMillisecond);
                        oos.close();

                        // write map
                        fout = new FileOutputStream(MAP_LOCATION);
                        oos = new ObjectOutputStream(fout);
                        oos.writeObject(MAP);
                        oos.close();
                        //&&& maybe do this so if one fails, the other fails too
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }//end of write if
            } // end of for
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @GET
    @Path("ping")
    public String getServerTime() {
        return "received ping on " + new Date().toString();
    }

    @GET
    @Path("/friends")
    @Produces({MediaType.APPLICATION_JSON})
    public FriendList getFriendsById(@QueryParam("id") String id) {
        Set<Friend> set = MAP.get(id);
        List<Friend> list = new ArrayList<>(set);
        FriendList friendList = new FriendList();
        friendList.setFriends(list);
        return friendList;
    }

}
