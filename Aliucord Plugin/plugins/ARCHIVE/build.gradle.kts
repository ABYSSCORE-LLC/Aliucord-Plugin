version = "0.2.0"
description = "Local-first backup archive manager for Aliucord"

aliucord {
    changelog.set(
        """
        # 0.2.0
        * Added the requested /back-up create, list, load, and delete subcommands.
        * Added a root-level project manifest for import discovery.
        * Uses private local JSON storage; no server scraping or restore is claimed.
        """.trimIndent(),
    )
    deploy.set(false)
}
