package com.kodu16.vsie.foundation;

import com.kodu16.vsie.vsie;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;

public final class VsieModelBakeryLogFilter {
    private static final String MODEL_BAKERY_LOGGER = "net.minecraft.client.resources.model.ModelBakery";
    private static final String VSIE_BLOCKSTATE_PREFIX = "Exception loading blockstate definition: '" + vsie.ID + ":";
    private static boolean installed = false;

    private VsieModelBakeryLogFilter() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        LoggerConfig loggerConfig = configuration.getLoggerConfig(MODEL_BAKERY_LOGGER);
        if (!MODEL_BAKERY_LOGGER.equals(loggerConfig.getName())) {
            LoggerConfig dedicatedLogger = new LoggerConfig(MODEL_BAKERY_LOGGER, Level.WARN, true);
            dedicatedLogger.setParent(loggerConfig);
            configuration.addLogger(MODEL_BAKERY_LOGGER, dedicatedLogger);
            loggerConfig = dedicatedLogger;
        }

        // Function: GeckoLib blocks intentionally skip vanilla blockstate models, so hide only VSIE's matching ModelBakery warnings.
        loggerConfig.addFilter(new AbstractFilter() {
            @Override
            public Filter.Result filter(LogEvent event) {
                if (!MODEL_BAKERY_LOGGER.equals(event.getLoggerName()) || event.getMessage() == null) {
                    return Result.NEUTRAL;
                }

                String message = event.getMessage().getFormattedMessage();
                return message.contains(VSIE_BLOCKSTATE_PREFIX) ? Result.DENY : Result.NEUTRAL;
            }
        });
        context.updateLoggers();
    }
}
