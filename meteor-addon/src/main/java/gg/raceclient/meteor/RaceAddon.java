package gg.raceclient.meteor;

import com.mojang.logging.LogUtils;
import gg.raceclient.meteor.modules.AutoPay;
import gg.raceclient.meteor.modules.TargetStrafe;
import gg.raceclient.meteor.modules.TargetTP;
import gg.raceclient.meteor.modules.VegaAura;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class RaceAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Race");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Race Meteor Addon");

        Modules.get().add(new AutoPay());
        Modules.get().add(new TargetTP());
        Modules.get().add(new VegaAura());
        Modules.get().add(new TargetStrafe());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "gg.raceclient.meteor";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("ItzSkater", "RaceClient");
    }
}
