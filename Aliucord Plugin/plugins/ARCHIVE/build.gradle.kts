version = "0.3.0"
description = "Local-first JSON backup archive manager for Aliucord"

aliucord {
    changelog.set(
        """
        # 0.3.0
        * Reworked the backup command handler and validation.
        * Added backup scope metadata (server, channel, or both).
        * Added inspect, list, load-preview, and delete operations.
        * Kept all backup data in Aliucord's private local files directory.
        * Does not claim to crawl or restore live Discord server state.
        """.trimIndent(),
    )
    deploy.set(false)
}
