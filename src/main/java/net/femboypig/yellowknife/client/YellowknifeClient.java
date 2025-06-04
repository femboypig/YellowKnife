package net.femboypig.yellowknife.client;

import net.fabricmc.api.ClientModInitializer;
import net.femboypig.yellowknife.util.AccountManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YellowknifeClient implements ClientModInitializer {
    public static final String MOD_ID = "yellowknife";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing YellowKnife account switcher");
        AccountManager.getInstance().loadAccounts();
    }
}
