package net.femboypig.yellowknife.client;

import net.fabricmc.api.ClientModInitializer;
import net.femboypig.yellowknife.util.AccountManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YellowknifeClient implements ClientModInitializer {
    public static final String MOD_ID = "yellowknife";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing YellowKnife account switcher");
        AccountManager.getInstance().loadAccounts();
    }
}
