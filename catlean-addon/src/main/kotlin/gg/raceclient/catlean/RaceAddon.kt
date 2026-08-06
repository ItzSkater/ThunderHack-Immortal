package gg.raceclient.catlean

import su.catlean.api.addon.CatLeanAddon
import su.catlean.api.addon.CatLeanApi

object RaceAddon : CatLeanAddon {
    lateinit var api: CatLeanApi

    override fun onCatLeanAddon(api: CatLeanApi) {
        this.api = api

        api.registry.registerModule(TargetTP)
        api.registry.registerModule(NoAimStrafe)
        api.registry.registerModule(TargetStrafe)

        api.logger("race").info("Race addon loaded: TargetTP, NoAimStrafe, TargetStrafe")
    }
}
