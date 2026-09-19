package com.nexuscraft.nexusendeavors;

/**
 * One entry in the daily or weekly objective pool -- config-defined, never hardcoded, so retuning
 * or adding an objective is a config.yml edit, not a rebuild. {@code objectiveKey} is the string
 * other systems (this plugin's own standalone listeners, or another Nexus plugin's EndeavorsBridge)
 * report progress against; several different objective ids can legally share the same
 * objectiveKey (a "quick" daily version and a bigger weekly version of the same underlying
 * activity, at different targets/rewards).
 */
record ObjectiveDefinition(String id, String objectiveKey, String description, int target, long rewardSeals) {
}
