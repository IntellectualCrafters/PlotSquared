package com.plotsquared.core.inject.modules;

import com.plotsquared.core.inject.annotations.PlotDatabase;
import org.jdbi.v3.guice.AbstractJdbiDefinitionModule;

public class JdbiModule extends AbstractJdbiDefinitionModule {

    public JdbiModule() {
        super(PlotDatabase.class);
    }
    @Override
    public void configureJdbi() {

    }
}
