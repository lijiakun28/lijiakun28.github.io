public class Main1 {
    public static void main(String[] args) {
        int[] weights = {2, 3, 4, 5};    // 物品重量
        int[] values = {3, 4, 5, 6};     // 物品价值
        int capacity = 8;                // 背包容量

        int maxValue = maxValueOptimized(weights, values, capacity);
        System.out.println("最大价值: " + maxValue);  // 输出: 最大价值: 10
    }

    public static int maxValueOptimized(int[] weights, int[] values, int capacity) {
        int[] dp = new int[capacity + 1];

        for (int i = 0; i < weights.length; i++) {
            for (int j = capacity; j >= weights[i]; j--) {
                dp[j] = Math.max(dp[j], dp[j - weights[i]] + values[i]);
            }
        }

        return dp[capacity];
    }
}