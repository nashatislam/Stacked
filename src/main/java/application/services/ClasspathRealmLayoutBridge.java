package application.services;

import application.Realm;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-realm assets + many nodes laid out in multiple parallel zig-zag lanes.
 * Guaranteed vertical spacing between lanes so markers don't overlap.
 * Nodes use normalized coordinates (0..1) and carry thresholds + resource URLs.
 */
public class ClasspathRealmLayoutBridge implements RealmLayoutService {

    @Override
    public Layout loadLayout(Realm realm) {
        String key = realm.name().toLowerCase();

        URL map  = find("/application/realm/" + key + "/map.png");
        if (map == null) map = find("/application/realm/map.png");

        URL node = find("/application/realm/" + key + "/node.png");
        if (node == null) node = find("/application/realm/node.png");

        List<NodeSpec> nodes = switch (realm) {
            case ALGORITHMS -> algorithms();
            case MEMORY     -> memory();
            case STACK      -> stack();
            case COOLING    -> cooling();
            case SYSTEMS    -> systems();
        };

        return new Layout(map, node, nodes);
    }

    private URL find(String path) { return getClass().getResource(path); }

    /* ─────────────────────── Geometry helpers (no overlap) ─────────────────────── */

    private record Pt(double x, double y) {}

    // Approx min normalized separation between two lane centers (≈ coin ~50–60px)
    private static final double MIN_DY = 0.12;     // vertical: keep lanes apart
    private static final double SAFE_TOP = 0.10;   // keep inside island
    private static final double SAFE_BOTTOM = 0.90;
    private static final double SAFE_LEFT = 0.12;
    private static final double SAFE_RIGHT = 0.88;

    /**
     * Build "lanes" (horizontal bands) centered around yCenter, with at least MIN_DY
     * separation, clamped safely inside the island silhouette.
     */
    private double[] makeBandsSpaced(int lanes, double yCenter) {
        double minSpan = (lanes - 1) * MIN_DY + 0.02;          // add tiny margin
        double span = Math.max(minSpan, 0.42);                 // ensure roomy by default
        double top = clamp(yCenter - span / 2, SAFE_TOP, SAFE_BOTTOM - span);
        double bottom = top + span;

        double[] b = new double[lanes];
        for (int i = 0; i < lanes; i++) {
            double t = (lanes == 1) ? 0.5 : (double) i / (lanes - 1);
            b[i] = top + t * (bottom - top);
        }
        return b;
    }

    /**
     * Produce N points across several columns and lanes.
     * Order is serpentine: col0 top→bottom, col1 bottom→top, …
     * Small per-lane horizontal offsets prevent intra-column stacking.
     */
    private List<Pt> multiLaneSerpentine(int n, int lanes, double yCenter) {
        double[] bands = makeBandsSpaced(lanes, yCenter);

        int cols = Math.max(2, (int) Math.ceil(n / (double) lanes));
        double dx = (SAFE_RIGHT - SAFE_LEFT) / Math.max(1, cols - 1);

        // Per-lane x offset to keep coins in the same column from sharing x exactly
        double laneXOffset = Math.min(0.018, dx * 0.28);

        List<Pt> out = new ArrayList<>(n);
        int remaining = n;
        for (int c = 0; c < cols && remaining > 0; c++) {
            double baseX = SAFE_LEFT + c * dx;

            int start = (c % 2 == 0) ? 0 : lanes - 1;
            int step  = (c % 2 == 0) ? 1 : -1;

            for (int k = 0; k < lanes && remaining > 0; k++) {
                int lane = start + k * step;

                // alternate offsets left/right per lane
                double x = baseX + ((lane % 2 == 0) ? -laneXOffset : laneXOffset);
                x = clamp(x, SAFE_LEFT + 0.01, SAFE_RIGHT - 0.01);

                double y = bands[lane]; // already safely spaced & clamped
                out.add(new Pt(x, y));
                remaining--;
            }
        }
        return out;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Convert (title,url) items into NodeSpecs laid out by the serpentine. */
    private List<NodeSpec> makeNodes(String idPrefix, String[][] items, int lanes, double yCenter, int xpStep) {
        int n = items.length;
        List<Pt> pts = multiLaneSerpentine(n, lanes, yCenter);

        List<NodeSpec> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id    = idPrefix + "-" + (i + 1);
            String title = items[i][0];
            String url   = items[i][1];
            int threshold = (i == 0) ? 0 : i * xpStep;
            String style = (i == 0) ? "node-unlocked" : (i == 1) ? "node-next" : "node-locked";
            String tip   = (i == 0) ? "Start here" : ("Unlock at " + threshold + " XP");
            Pt p = pts.get(i);
            out.add(new NodeSpec(id, p.x, p.y, title, tip, style, threshold, url));
        }
        return out;
    }

    /* ─────────────────────────── Realms with more lanes ─────────────────────────── */

    /** Algorithms — many problems, 6 lanes, a little lower on the island. */
    private List<NodeSpec> algorithms() {
        String[][] items = {
                {"Two Sum","https://leetcode.com/problems/two-sum/"},
                {"Contains Duplicate","https://leetcode.com/problems/contains-duplicate/"},
                {"Valid Anagram","https://leetcode.com/problems/valid-anagram/"},
                {"Best Time I","https://leetcode.com/problems/best-time-to-buy-and-sell-stock/"},
                {"Product Except Self","https://leetcode.com/problems/product-of-array-except-self/"},
                {"Maximum Subarray","https://leetcode.com/problems/maximum-subarray/"},
                {"Merge Intervals","https://leetcode.com/problems/merge-intervals/"},
                {"3Sum","https://leetcode.com/problems/3sum/"},
                {"Group Anagrams","https://leetcode.com/problems/group-anagrams/"},
                {"Binary Search","https://leetcode.com/problems/binary-search/"},
                {"Number of Islands","https://leetcode.com/problems/number-of-islands/"},
                {"Top K Frequent","https://leetcode.com/problems/top-k-frequent-elements/"},
                {"K Closest Points","https://leetcode.com/problems/k-closest-points-to-origin/"},
                {"Course Schedule","https://leetcode.com/problems/course-schedule/"},
                {"Word Ladder","https://leetcode.com/problems/word-ladder/"},
                {"Min Window Substring","https://leetcode.com/problems/minimum-window-substring/"},
                {"Coin Change","https://leetcode.com/problems/coin-change/"},
                {"LIS","https://leetcode.com/problems/longest-increasing-subsequence/"},
                {"Rotting Oranges","https://leetcode.com/problems/rotting-oranges/"},
                {"Pacific Atlantic","https://leetcode.com/problems/pacific-atlantic-water-flow/"},
                {"Reorder List","https://leetcode.com/problems/reorder-list/"},
                {"Longest Palindromic Substring","https://leetcode.com/problems/longest-palindromic-substring/"},
                {"Subsets","https://leetcode.com/problems/subsets/"},
                {"Permutations","https://leetcode.com/problems/permutations/"},
                {"Climbing Stairs","https://leetcode.com/problems/climbing-stairs/"},
                {"House Robber","https://leetcode.com/problems/house-robber/"},
                {"Jump Game","https://leetcode.com/problems/jump-game/"},
                {"Valid Sudoku","https://leetcode.com/problems/valid-sudoku/"},
                {"Median of Two Sorted Arrays (read)","https://leetcode.com/problems/median-of-two-sorted-arrays/"},
                {"Edit Distance (hard)","https://leetcode.com/problems/edit-distance/"}
        };
        return makeNodes("alg", items, 6, 0.62, 120);
    }

    /** Memory / Design / Data structures — 5 lanes, middle of island. */
    private List<NodeSpec> memory() {
        String[][] items = {
                {"Copy Random List","https://leetcode.com/problems/copy-list-with-random-pointer/"},
                {"Design HashMap","https://leetcode.com/problems/design-hashmap/"},
                {"LRU Cache","https://leetcode.com/problems/lru-cache/"},
                {"LFU Cache","https://leetcode.com/problems/lfu-cache/"},
                {"Clone Graph","https://leetcode.com/problems/clone-graph/"},
                {"Serialize/Deserialize Tree","https://leetcode.com/problems/serialize-and-deserialize-binary-tree/"},
                {"Implement Trie","https://leetcode.com/problems/implement-trie-prefix-tree/"},
                {"Time Map","https://leetcode.com/problems/time-based-key-value-store/"},
                {"All O(1) Structure","https://leetcode.com/problems/all-oone-data-structure/"},
                {"Snapshot Array","https://leetcode.com/problems/snapshot-array/"},
                {"Min Stack","https://leetcode.com/problems/min-stack/"},
                {"Design Circular Queue","https://leetcode.com/problems/design-circular-queue/"},
                {"Random Pick w/Weight","https://leetcode.com/problems/random-pick-with-weight/"},
                {"Kth Largest Stream","https://leetcode.com/problems/kth-largest-element-in-a-stream/"},
                {"Design Skiplist","https://leetcode.com/problems/design-skiplist/"},
                {"File System","https://leetcode.com/problems/design-file-system/"},
                {"Browser History","https://leetcode.com/problems/design-browser-history/"},
                {"Logger Rate Limiter","https://leetcode.com/problems/logger-rate-limiter/"},
                {"Prefix/Suffix Search","https://leetcode.com/problems/prefix-and-suffix-search/"},
                {"Design Twitter","https://leetcode.com/problems/design-twitter/"},
                {"Randomized Set","https://leetcode.com/problems/insert-delete-getrandom-o1/"},
                {"TinyURL","https://leetcode.com/problems/encode-and-decode-tinyurl/"},
                {"Authentication Manager","https://leetcode.com/problems/design-authentication-manager/"},
                {"LRU (Review)","https://leetcode.com/problems/lru-cache/"}
        };
        return makeNodes("mem", items, 5, 0.54, 140);
    }

    /** Stack / Queue / Monotonic — 5 lanes, a bit lower to follow the south coast. */
    private List<NodeSpec> stack() {
        String[][] items = {
                {"Min Stack","https://leetcode.com/problems/min-stack/"},
                {"Queue via Stacks","https://leetcode.com/problems/implement-queue-using-stacks/"},
                {"Evaluate RPN","https://leetcode.com/problems/evaluate-reverse-polish-notation/"},
                {"Daily Temperatures","https://leetcode.com/problems/daily-temperatures/"},
                {"Next Greater II","https://leetcode.com/problems/next-greater-element-ii/"},
                {"Car Fleet","https://leetcode.com/problems/car-fleet/"},
                {"Largest Rectangle","https://leetcode.com/problems/largest-rectangle-in-histogram/"},
                {"Trapping Rain Water","https://leetcode.com/problems/trapping-rain-water/"},
                {"Remove K Digits","https://leetcode.com/problems/remove-k-digits/"},
                {"Decode String","https://leetcode.com/problems/decode-string/"},
                {"Simplify Path","https://leetcode.com/problems/simplify-path/"},
                {"Basic Calculator II","https://leetcode.com/problems/basic-calculator-ii/"},
                {"Min Add Parens","https://leetcode.com/problems/minimum-add-to-make-parentheses-valid/"},
                {"Next Greater I","https://leetcode.com/problems/next-greater-element-i/"},
                {"Remove Adj Dups","https://leetcode.com/problems/remove-all-adjacent-duplicates-in-string/"},
                {"Asteroid Collision","https://leetcode.com/problems/asteroid-collision/"},
                {"Backspace Compare","https://leetcode.com/problems/backspace-string-compare/"},
                {"Open the Lock (BFS)","https://leetcode.com/problems/open-the-lock/"},
                {"Shortest Path Binary Matrix","https://leetcode.com/problems/shortest-path-in-binary-matrix/"},
                {"Parsing Boolean Expr","https://leetcode.com/problems/parsing-a-boolean-expression/"},
                {"Monotonic Template (read)","https://leetcode.com/tag/monotonic-stack/"}
        };
        return makeNodes("ds", items, 5, 0.60, 135);
    }

    /** Cooling (Optimization) — Greedy / DP / Graphs; 5 lanes slightly upper-mid. */
    private List<NodeSpec> cooling() {
        String[][] items = {
                {"Max Subarray (Kadane)","https://leetcode.com/problems/maximum-subarray/"},
                {"Best Time I","https://leetcode.com/problems/best-time-to-buy-and-sell-stock/"},
                {"Best Time II","https://leetcode.com/problems/best-time-to-buy-and-sell-stock-ii/"},
                {"Jump Game","https://leetcode.com/problems/jump-game/"},
                {"Jump Game II","https://leetcode.com/problems/jump-game-ii/"},
                {"Partition Equal Sum","https://leetcode.com/problems/partition-equal-subset-sum/"},
                {"House Robber","https://leetcode.com/problems/house-robber/"},
                {"House Robber II","https://leetcode.com/problems/house-robber-ii/"},
                {"Coin Change","https://leetcode.com/problems/coin-change/"},
                {"Coin Change II","https://leetcode.com/problems/coin-change-ii/"},
                {"LIS","https://leetcode.com/problems/longest-increasing-subsequence/"},
                {"Min Cost Climbing Stairs","https://leetcode.com/problems/min-cost-climbing-stairs/"},
                {"Unique Paths","https://leetcode.com/problems/unique-paths/"},
                {"Decode Ways","https://leetcode.com/problems/decode-ways/"},
                {"Ones and Zeroes (0/1)","https://leetcode.com/problems/ones-and-zeroes/"},
                {"Last Stone Weight II","https://leetcode.com/problems/last-stone-weight-ii/"},
                {"Cheapest Flights K Stops","https://leetcode.com/problems/cheapest-flights-within-k-stops/"},
                {"Network Delay Time","https://leetcode.com/problems/network-delay-time/"},
                {"Min Path Sum","https://leetcode.com/problems/minimum-path-sum/"},
                {"Maximal Square","https://leetcode.com/problems/maximal-square/"},
                {"Russian Doll Envelopes","https://leetcode.com/problems/russian-doll-envelopes/"},
                {"Dijkstra (read)","https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm"}
        };
        return makeNodes("opt", items, 5, 0.48, 130);
    }

    /** Systems — reading/design resources; 5 lanes upper-middle/right. */
    private List<NodeSpec> systems() {
        String[][] items = {
                {"Design Primer","https://github.com/donnemartin/system-design-primer"},
                {"Caching Strategies","https://github.com/donnemartin/system-design-primer#caching"},
                {"Load Balancing","https://github.com/donnemartin/system-design-primer#load-balancer"},
                {"Rate Limiting","https://github.com/donnemartin/system-design-primer#rate-limiting"},
                {"Content Delivery (CDN)","https://github.com/donnemartin/system-design-primer#content-delivery-network"},
                {"Database Sharding","https://github.com/donnemartin/system-design-primer#sharding"},
                {"Consistent Hashing","https://en.wikipedia.org/wiki/Consistent_hashing"},
                {"Message Queues","https://github.com/donnemartin/system-design-primer#message-queues"},
                {"Search Autosuggest","https://github.com/donnemartin/system-design-primer#typeahead-suggestion"},
                {"URL Shortener","https://github.com/donnemartin/system-design-primer#url-shortener"},
                {"Design Twitter Feed","https://github.com/donnemartin/system-design-primer#design-a-twitter-timeline"},
                {"News Feed","https://github.com/donnemartin/system-design-primer#design-a-social-media-feed"},
                {"Chat System","https://github.com/donnemartin/system-design-primer#design-a-chat-system"},
                {"File Storage","https://github.com/donnemartin/system-design-primer#design-a-file-storage-system"},
                {"Image Hosting","https://github.com/donnemartin/system-design-primer#design-an-image-hosting-service-like-imgur"},
                {"Pastebin","https://github.com/donnemartin/system-design-primer#pastebin"},
                {"Instagram","https://github.com/donnemartin/system-design-primer#design-instagram"},
                {"Dropbox","https://github.com/donnemartin/system-design-primer#design-a-file-sync-service-like-dropbox"},
                {"Uber","https://github.com/donnemartin/system-design-primer#design-uber"},
                {"YouTube","https://github.com/donnemartin/system-design-primer#design-youtube"}
        };
        return makeNodes("sys", items, 5, 0.46, 150);
    }
}
