package com.aguo.flowlimit.core.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wenqiaogang
 * @DateTime: 2022/8/25 17:42
 * @Description: TODO
 */
@Slf4j
public class ShowUtil {
    /**
     * 当有多个流量限制器实现类，为了避免重复展示，当展示完banner，即设置为TRUE。
     */
    private static boolean isReady = false;

    /**
     * 展示启动成功的banner。
     */
    public static void showBanner() {
        if (!isReady) {
            log.info("\n _______  __        ______   ____    __    ____     __       __  .___  ___.  __  .___________.\n" +
                    "|   ____||  |      /  __  \\  \\   \\  /  \\  /   /    |  |     |  | |   \\/   | |  | |           |\n" +
                    "|  |__   |  |     |  |  |  |  \\   \\/    \\/   /     |  |     |  | |  \\  /  | |  | `---|  |----`\n" +
                    "|   __|  |  |     |  |  |  |   \\            /      |  |     |  | |  |\\/|  | |  |     |  |     \n" +
                    "|  |     |  `----.|  `--'  |    \\    /\\    /       |  `----.|  | |  |  |  | |  |     |  |     \n" +
                    "|__|     |_______| \\______/      \\__/  \\__/        |_______||__| |__|  |__| |__|     |__|     \n");
            isReady = true;
        }
    }
}
