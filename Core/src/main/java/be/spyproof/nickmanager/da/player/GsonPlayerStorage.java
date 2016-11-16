package be.spyproof.nickmanager.da.player;

import be.spyproof.nickmanager.model.PlayerData;
import be.spyproof.nickmanager.util.Reference;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.util.*;

/**
 * Created by Spyproof on 03/11/2016.
 */
public class GsonPlayerStorage implements IPlayerStorage
{
    private static final String EXTENSION = ".json";
    private File playerDir;

    private Map<String, UUID> cachedPlayers = new TreeMap<>();
    private Map<String, List<UUID>> cachedNicknames = new HashMap<>();

    public GsonPlayerStorage(File playerDir) throws IOException
    {
        this.playerDir = playerDir;
        if (!playerDir.exists())
            playerDir.mkdirs();
        loadPlayerCache();
    }

    private void loadPlayerCache() throws IOException
    {
        File[] playerFiles = playerDir.listFiles();
        if (playerFiles == null)
            return;

        for (File file : playerFiles)
        {
            Optional<PlayerData> playerData = readPlayer(new JsonReader(new InputStreamReader(new FileInputStream(file), "UTF-8")), UUID.fromString(file.getName().replace(EXTENSION, "")));
            if (playerData.isPresent())
                addToCache(playerData.get());
        }
    }

    private void addToCache(PlayerData playerData)
    {
        this.cachedPlayers.put(playerData.getName(), playerData.getUuid());
        if (playerData.getNickname().isPresent())
        {
            List<UUID> set = this.cachedNicknames.get(playerData.getNickname().get());
            if (set == null)
            {
                set = new ArrayList<>();
                set.add(playerData.getUuid());
                this.cachedNicknames.put(playerData.getNickname().get().replaceAll(Reference.COLOUR_AND_STYLE_PATTERN, ""), set);
            }else if (!set.contains(playerData.getUuid()))
                set.add(playerData.getUuid());
        }
    }

    @Override
    public void savePlayer(PlayerData player)
    {
        try
        {
            writePlayer(player);
            addToCache(player);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void removePlayer(PlayerData player)
    {
        removePlayerFile(player.getUuid());
    }

    @Override
    public Optional<PlayerData> getPlayer(String name)
    {
        UUID uuid = this.cachedPlayers.get(name);
        if (uuid == null)
            return Optional.empty();
        else
            return getPlayer(uuid);
    }

    @Override
    public Optional<PlayerData> getPlayer(UUID uuid)
    {
        try
        {
            return readPlayer(uuid);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public List<PlayerData> getPlayerByNickname(String nickname, int limit)
    {
        List<PlayerData> players = new ArrayList<>();

        for (Map.Entry<String, List<UUID>> entry : this.cachedNicknames.entrySet())
        {
            if (entry.getKey().toLowerCase().contains(nickname.toLowerCase()))
            {
                for (UUID uuid : entry.getValue())
                {
                    Optional<PlayerData> playerData = getPlayer(uuid);
                    if (playerData.isPresent())
                    {
                        boolean isDuplicate = false;
                        for (PlayerData p : players)
                            if (p.getUuid().equals(playerData.get().getUuid()))
                                isDuplicate = true;

                        if (!isDuplicate)
                            players.add(playerData.get());
                    }
                }
            }
        }

        return players;
    }

    @Override
    public void close() throws IOException
    {

    }

    private void removePlayerFile(UUID uuid)
    {
        File file = new File(playerDir, uuid.toString() + EXTENSION);
        if (file.exists())
            file.delete();
    }

    private void writePlayer(PlayerData player) throws IOException
    {
        File file = new File(playerDir, player.getUuid().toString() + EXTENSION);
        if (!file.exists())
            file.createNewFile();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        writer.setIndent("    ");
        writePlayer(writer, player);
        writer.close();
    }

    private void writePlayer(JsonWriter writer, PlayerData player) throws IOException
    {
        writer.beginObject();
        writer.name("Last known name").value(player.getName());
        writer.name("Tokens").value(player.getTokensRemaining());
        writer.name("Last changed").value(player.getLastChanged());
        writer.name("Accepted rules").value(player.hasAcceptedRules());
        if (player.getNickname().isPresent())
            writer.name("Nickname").value(player.getNickname().get());
        else
            writer.name("Nickname").nullValue();

        writer.name("Past nicknames").beginArray();

        for (String nick : player.getPastNicknames())
            writer.value(nick);

        writer.endArray();
        writer.endObject();
    }

    private Optional<PlayerData> readPlayer(UUID uuid) throws IOException
    {
        File file = new File(playerDir, uuid.toString() + EXTENSION);
        if (!file.exists())
            return Optional.empty();

        try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(file), "UTF-8")))
        {
            return readPlayer(reader, uuid);
        }
    }

    private Optional<PlayerData> readPlayer(JsonReader reader, UUID uuid)throws IOException
    {
        reader.beginObject();
        String playerName = null, nickname = null;
        int tokens = 0;
        long lastChanged = 0;
        boolean acceptedRules = false;
        List<String> pastNicknames = new ArrayList<>();

        while (reader.hasNext())
        {
            String name = reader.nextName();
            if (name.equals("Last known name"))
                playerName = reader.nextString();
            else if (name.equals("Nickname") && reader.peek() != JsonToken.NULL)
                nickname = reader.nextString();
            else if (name.equals("Tokens"))
                tokens = reader.nextInt();
            else if (name.equals("Last changed"))
                lastChanged = reader.nextLong();
            else if (name.equals("Accepted rules"))
                acceptedRules = reader.nextBoolean();
            else if (name.equals("Past nicknames"))
            {
                reader.beginArray();

                while (reader.hasNext())
                    pastNicknames.add(reader.nextString());

                reader.endArray();
            }else
                reader.skipValue();
        }

        reader.endObject();
        if (playerName == null)
            return Optional.empty();

        PlayerData playerData = new PlayerData(playerName, uuid);

        playerData.setTokensRemaining(tokens);
        playerData.setLastChanged(lastChanged);
        playerData.setAcceptedRules(acceptedRules);
        if (nickname != null)
            playerData.setNickname(nickname);
        if (pastNicknames.size() > 0)
            playerData.addPastNickname(pastNicknames);

        return Optional.of(playerData);
    }

}
