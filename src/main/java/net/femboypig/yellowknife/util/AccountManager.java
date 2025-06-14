package net.femboypig.yellowknife.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.femboypig.yellowknife.client.YellowknifeClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public class AccountManager {
    private static AccountManager instance;
    private final List<Account> accounts = new ArrayList<>();
    private final File accountsFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private AccountManager() {
        File runDir = MinecraftClient.getInstance().runDirectory;
        File configDir = new File(runDir, "config");
        if (!configDir.exists()) {
            configDir.mkdir();
        }
        File modConfigDir = new File(configDir, "yellowknife");
        if (!modConfigDir.exists()) {
            modConfigDir.mkdir();
        }
        accountsFile = new File(modConfigDir, "accounts.json");
    }

    public static AccountManager getInstance() {
        if (instance == null) {
            instance = new AccountManager();
        }
        return instance;
    }

    public void loadAccounts() {
        accounts.clear();
        if (accountsFile.exists()) {
            try (FileReader reader = new FileReader(accountsFile)) {
                Type accountListType = new TypeToken<ArrayList<Account>>() {}.getType();
                List<Account> loadedAccounts = gson.fromJson(reader, accountListType);
                if (loadedAccounts != null) {
                    accounts.addAll(loadedAccounts);
                    YellowknifeClient.LOGGER.info("Loaded " + accounts.size() + " accounts");
                }
            } catch (IOException e) {
                YellowknifeClient.LOGGER.error("Failed to load accounts", e);
            }
        }
    }

    public void saveAccounts() {
        try (FileWriter writer = new FileWriter(accountsFile)) {
            gson.toJson(accounts, writer);
        } catch (IOException e) {
            YellowknifeClient.LOGGER.error("Failed to save accounts", e);
        }
    }

    public void addAccount(String username) {
        // Create a new offline account with a UUID based on the username
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
        Account account = new Account(username, uuid.toString().replace("-", ""), new Date());
        accounts.add(account);
        saveAccounts();
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        saveAccounts();
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public boolean switchToAccount(Account account) {
        try {
            // For Minecraft 1.21.5, Session constructor parameters have changed
            // Convert the UUID string to a proper UUID object
            UUID accountUUID = UUID.fromString(
                account.uuid.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", 
                    "$1-$2-$3-$4-$5"
                )
            );
            
            Session newSession = new Session(
                account.username,  // username
                accountUUID,       // uuid as UUID object
                "",                // accessToken (empty for offline)
                Optional.empty(),  // xboxUserId (empty for offline)
                Optional.empty(),  // clientId (empty for offline)
                Session.AccountType.MOJANG // account type
            );
            
            // В Minecraft 1.21.5, имя поля session могло измениться
            // Поиск всех полей типа Session в классе MinecraftClient
            boolean foundField = false;
            for (Field field : MinecraftClient.class.getDeclaredFields()) {
                if (field.getType() == Session.class) {
                    YellowknifeClient.LOGGER.info("Found session field: " + field.getName());
                    field.setAccessible(true);
                    field.set(MinecraftClient.getInstance(), newSession);
                    YellowknifeClient.LOGGER.info("Switched to account: " + account.username);
                    foundField = true;
                    break;
                }
            }
            
            if (!foundField) {
                YellowknifeClient.LOGGER.error("Could not find Session field in MinecraftClient");
                return false;
            }
            
            // Обновляем дату последнего входа
            account.lastLoginDate = new Date();
            saveAccounts();
            
            return true;
        } catch (Exception e) {
            YellowknifeClient.LOGGER.error("Failed to switch accounts", e);
            return false;
        }
    }

    public static class Account {
        private final String username;
        private final String uuid;
        private Date creationDate;
        private Date lastLoginDate;

        public Account(String username, String uuid, Date creationDate) {
            this.username = username;
            this.uuid = uuid;
            this.creationDate = creationDate;
            this.lastLoginDate = creationDate;
        }

        public String getUsername() {
            return username;
        }

        public String getUuid() {
            return uuid;
        }

        public Date getCreationDate() {
            return creationDate;
        }

        public Date getLastLoginDate() {
            return lastLoginDate;
        }

        public String getFormattedCreationDate() {
            return DATE_FORMAT.format(creationDate);
        }

        public String getFormattedLastLoginDate() {
            return DATE_FORMAT.format(lastLoginDate);
        }
    }
} 