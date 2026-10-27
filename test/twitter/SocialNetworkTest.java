/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * Testing strategy for SocialNetwork
     *
     * guessFollowsGraph(tweets) -> followsGraph
     *
     * Partition on the tweets:
     *   - empty list
     *   - nonempty list, no @-mentions
     *   - one tweet with one @-mention
     *   - one tweet with several @-mentions
     *   - several tweets by the same author
     *   - @-mention of the author (self-mention)
     *   - duplicate mentions of the same user, differing only in case
     *   - mixed-case author and mention
     *
     * Partition on the output graph:
     *   - an author with no evidence of following anybody: the key may be
     *     absent or map to the empty set
     *   - an author with one or more mentioned users
     *
     * The postcondition is underdetermined: an implementation may infer extra
     * follows from other evidence, so the tests assert that the required edges
     * are present (and that clearly unsupported edges are absent), never that
     * the whole map equals one particular map.
     *
     * influencers(followsGraph) -> list
     *
     * Partition on the input graph:
     *   - empty graph
     *   - one user, no followers
     *   - one user followed by everybody else
     *   - several users with distinct follower counts
     *   - several users tied on follower count
     *
     * The order of users with equal follower counts is unspecified, so ties are
     * checked as a set and only the descending follower-count order is asserted.
     */

    private static final Instant T0 = Instant.parse("2026-10-23T09:00:00Z");

    private static Tweet tweet(long id, String author, String text) {
        return new Tweet(id, author, text, T0.plusSeconds(id));
    }

    private static boolean follows(Map<String, Set<String>> graph, String follower, String followed) {
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(follower)) {
                for (String name : entry.getValue()) {
                    if (name.equalsIgnoreCase(followed)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Set<String> lowerCaseNames(Set<String> names) {
        Set<String> result = new HashSet<>();
        for (String name : names) {
            result.add(name.toLowerCase(Locale.ROOT));
        }
        return result;
    }

    private static Set<String> lowerCaseKeys(Map<String, Set<String>> graph) {
        return lowerCaseNames(graph.keySet());
    }

    private static int indexOfIgnoreCase(List<String> names, String wanted) {
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equalsIgnoreCase(wanted)) {
                return i;
            }
        }
        return -1;
    }

    private static Set<String> setOf(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // Empty list of tweets: the graph must be empty.
    @Test
    public void testGuessFollowsGraphEmptyList() {
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(new ArrayList<>());
        assertTrue("expected empty graph", graph.isEmpty());
    }

    // Tweets without mentions: no author has evidence of following anyone.
    @Test
    public void testGuessFollowsGraphNoMentions() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "alice", "Lunch was great today."),
                tweet(2, "bob", "No mentions here, just plain words."));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        for (Set<String> follows : graph.values()) {
            assertTrue("expected no inferred follows", follows.isEmpty());
        }
        assertTrue("only tweet authors may appear as keys",
                setOf("alice", "bob").containsAll(lowerCaseKeys(graph)));
    }

    // Single mention: ernie follows bert, and nobody follows ernie.
    @Test
    public void testGuessFollowsGraphSingleMention() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "ernie", "Hey @bert, how are you?"));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue("ernie should follow bert", follows(graph, "ernie", "bert"));
        assertFalse("bert should not follow ernie", follows(graph, "bert", "ernie"));
        assertFalse("users cannot follow themselves", follows(graph, "ernie", "ernie"));
        assertEquals("only ernie and bert may appear", setOf("ernie", "bert"),
                lowerCaseNames(allNames(graph)));
    }

    // Multiple mentions in one tweet: both mentioned users are followed.
    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ernie", "@bert and @cookie_monster should meet @oskar."));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(follows(graph, "ernie", "bert"));
        assertTrue(follows(graph, "ernie", "cookie_monster"));
        assertTrue(follows(graph, "ernie", "oskar"));
        assertEquals("every name must be an author or a mention", setOf("ernie", "bert",
                "cookie_monster", "oskar"), lowerCaseNames(allNames(graph)));
    }

    // Multiple tweets from one user: the union of all mentioned users is kept.
    @Test
    public void testGuessFollowsGraphMultipleTweetsFromOneUser() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ernie", "@bert, did you see this?"),
                tweet(2, "ernie", "Talking to @cookie_monster again."),
                tweet(3, "ernie", "No mention in this one."));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(follows(graph, "ernie", "bert"));
        assertTrue(follows(graph, "ernie", "cookie_monster"));
        assertFalse(follows(graph, "ernie", "ernie"));
    }

    // Empty graph for influencers(): the result is an empty list.
    @Test
    public void testInfluencersEmptyGraph() {
        Map<String, Set<String>> graph = new HashMap<>();
        List<String> influencers = SocialNetwork.influencers(graph);
        assertTrue("expected empty list", influencers.isEmpty());
    }

    // Single user without followers: the user is still in the graph, so the
    // spec requires that the user appears, with zero followers, in the list.
    @Test
    public void testInfluencersSingleUserWithoutFollowers() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("alice", new HashSet<>());

        List<String> influencers = SocialNetwork.influencers(graph);

        assertEquals("all usernames in the graph must be returned", 1, influencers.size());
        assertEquals("alice", influencers.get(0));
    }

    // Single influencer: the only user with a follower is first.
    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("alice", setOf("bob"));
        graph.put("carol", setOf("bob"));

        List<String> influencers = SocialNetwork.influencers(graph);

        assertEquals(3, influencers.size());
        assertEquals("bob", influencers.get(0));
        assertTrue(indexOfIgnoreCase(influencers, "alice") > 0);
        assertTrue(indexOfIgnoreCase(influencers, "carol") > 0);
    }

    // Multiple influencers: distinct follower counts give a strict order.
    @Test
    public void testInfluencersMultipleInfluencers() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("alice", setOf("bob"));
        graph.put("carol", setOf("bob"));
        graph.put("dave", setOf("bob", "erin"));

        List<String> influencers = SocialNetwork.influencers(graph);

        assertEquals(5, influencers.size());
        assertEquals("bob", influencers.get(0));
        assertEquals("erin", influencers.get(1));
        assertEquals(setOf("alice", "carol", "dave"), lowerCaseNames(
                new HashSet<>(influencers.subList(2, 5))));
    }

    // Tied influence: equal follower counts may appear in any order.
    @Test
    public void testInfluencersTiedInfluence() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("alice", setOf("bob"));
        graph.put("carol", setOf("dave"));

        List<String> influencers = SocialNetwork.influencers(graph);

        assertEquals(4, influencers.size());
        assertEquals(setOf("bob", "dave"), lowerCaseNames(
                new HashSet<>(influencers.subList(0, 2))));
        assertEquals(setOf("alice", "carol"), lowerCaseNames(
                new HashSet<>(influencers.subList(2, 4))));
    }

    // Usernames are case-insensitive: Ernie and ERNIE are the same user.
    @Test
    public void testGuessFollowsGraphMentionIsCaseInsensitive() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "Ernie", "Hello @BERT and @Bert!"));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue("ernie should follow bert", follows(graph, "ernie", "bert"));
        int bertEdges = 0;
        for (Set<String> follows : graph.values()) {
            for (String name : follows) {
                if (name.equalsIgnoreCase("bert")) {
                    bertEdges++;
                }
            }
        }
        assertEquals("duplicate mentions must collapse to one edge", 1, bertEdges);
        assertEquals("Ernie and ernie are the same key", setOf("ernie", "bert"),
                lowerCaseNames(allNames(graph)));
    }

    // A self-mention is not evidence of following anybody.
    @Test
    public void testGuessFollowsGraphSelfMentionDoesNotCreateEdge() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "ernie", "Talking to myself, @ernie."));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertFalse("users cannot follow themselves", follows(graph, "ernie", "ernie"));
    }

    // An author with no mentions may be present with an empty set or absent;
    // either is legal, so the test only requires no self-edge and no invented user.
    @Test
    public void testGuessFollowsGraphAuthorWithoutFollows() {
        List<Tweet> tweets = Arrays.asList(tweet(1, "quiet_user", "Just reading today."));

        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);

        assertFalse(follows(graph, "quiet_user", "quiet_user"));
        assertTrue("no invented users", setOf("quiet_user")
                .containsAll(lowerCaseNames(allNames(graph))));
    }

    private static Set<String> allNames(Map<String, Set<String>> graph) {
        Set<String> names = new HashSet<>(graph.keySet());
        for (Set<String> follows : graph.values()) {
            names.addAll(follows);
        }
        return names;
    }
}
