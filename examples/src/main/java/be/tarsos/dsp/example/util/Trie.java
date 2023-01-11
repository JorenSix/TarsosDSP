package be.tarsos.dsp.example.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A trie that can be used for auto completion
 * @author Marcus McCurdy (marcus.mccurdy@gmail.com)
 */
public class Trie {

    final Map<Character, Trie> children;
    String value;
    boolean terminal = false;

    /**
     * Creates an empty constructor
     */
    public Trie() {
        this(null);
    }

    private Trie(String value) {
        this.value = value;
        children = new HashMap<Character, Trie>();
    }

    void add(char c) {
        String val;
        if (this.value == null) {
            val = Character.toString(c);
        } else {
            val = this.value + c;
        }
        children.put(c, new Trie(val));
    }

    /**
     * Add a word to the trie
     * @param word the word or prefix to add
     */
    public void insert(String word) {
        if (word == null) {
            throw new IllegalArgumentException("Cannot add null to a Trie");
        }
        Trie node = this;
        for (char c : word.toCharArray()) {
            if (!node.children.containsKey(c)) {
                node.add(c);
            }
            node = node.children.get(c);
        }
        node.terminal = true;
    }

    /**
     * Tries to find a word in the Trie
     * @param word The word to search for
     * @return Either an empty string or the found word.
     */
    public String find(String word) {
        Trie node = this;
        for (char c : word.toCharArray()) {
            if (!node.children.containsKey(c)) {
                return "";
            }
            node = node.children.get(c);
        }
        return node.value;
    }

    /**
     * Complete a string with the prefixes from the
     * @param prefix the prefix to expand.
     * @return an expanded prefix.
     */
    public Collection<String> autoComplete(String prefix) {
        Trie node = this;
        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) {
                return Collections.emptyList();
            }
            node = node.children.get(c);
        }
        return node.allPrefixes();
    }

    Collection<String> allPrefixes() {
        List<String> results = new ArrayList<String>();
        if (this.terminal) {
            results.add(this.value);
        }
        for (Entry<Character, Trie> entry : children.entrySet()) {
            Trie child = entry.getValue();
            Collection<String> childPrefixes = child.allPrefixes();
            results.addAll(childPrefixes);
        }
        return results;
    }
}
