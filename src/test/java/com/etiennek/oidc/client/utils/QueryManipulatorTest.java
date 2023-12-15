package com.etiennek.oidc.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import static com.etiennek.oidc.client.utils.UriUtils.*;

public class QueryManipulatorTest {
    @Test
    void constructor() {
        // Empty
        assertEquals("", new QueryManipulator().toQueryString());
        assertEquals("", new QueryManipulator("").toQueryString());
        assertEquals("", new QueryManipulator("?").toQueryString());
        assertEquals("", new QueryManipulator(" ").toQueryString());
        assertEquals("", new QueryManipulator((String) null).toQueryString());
        assertEquals("",
                new QueryManipulator(toUrl("http://www.example.com")).toQueryString());
        assertEquals("",
                new QueryManipulator(toUrl("http://www.example.com?")).toQueryString());

        // Populated
        assertEquals("a=b&foo=bar", new QueryManipulator("foo=bar&a=b").toQueryString());
        assertEquals("a=b&foo=bar", new QueryManipulator("?foo=bar&a=b").toQueryString());
        assertEquals("a=&foo=bar", new QueryManipulator("?foo=bar&a").toQueryString());
        assertEquals("a=b&foo=bar",
                new QueryManipulator(toUrl("http://www.example.com?foo=bar&a=b")).toQueryString());
        assertEquals("a=&foo=bar",
                new QueryManipulator(toUrl("http://www.example.com?foo=bar&a")).toQueryString());
        assertEquals("=bar&a=",
                new QueryManipulator(toUrl("http://www.example.com?=bar&a")).toQueryString());
        assertEquals("=bar&a=",
                new QueryManipulator(toUrl("http://www.example.com?=bar&a=")).toQueryString());
    }

    @Test
    void Should_encode_keys_and_values() {
        var toTest = new QueryManipulator("f%25o=bar&a=b%25%25%25t");

        toTest.add("!@#$%^&()_+{}[]\\|/><\"':;", "!@#$%^&()_+{}[]\\|/><\"':;");
        assertEquals(
                "%21%40%23%24%25%5E%26%28%29_%2B%7B%7D%5B%5D%5C%7C%2F%3E%3C%22%27%3A%3B=%21%40%23%24%25%5E%26%28%29_%2B%7B%7D%5B%5D%5C%7C%2F%3E%3C%22%27%3A%3B&a=b%25%25%25t&f%25o=bar",
                toTest.toQueryString());
    }

    @Test
    void add() {
        var toTestFromEmpty = new QueryManipulator();
        toTestFromEmpty.add("key", "value");
        assertEquals("key=value", toTestFromEmpty.toQueryString());
        toTestFromEmpty.add("akey", "avalue");
        assertEquals("akey=avalue&key=value", toTestFromEmpty.toQueryString());
        toTestFromEmpty.add("key", "value");
        assertEquals("akey=avalue&key=value&key=value", toTestFromEmpty.toQueryString());

        var toTest = new QueryManipulator("foo=bar&a=b");
        toTest.add("z", "zval");
        assertEquals("a=b&foo=bar&z=zval", toTest.toQueryString());
        toTest.add("z", "anotherzval");
        assertEquals("a=b&foo=bar&z=anotherzval&z=zval", toTest.toQueryString());
        toTest.add("a", "anotheraval");
        assertEquals("a=anotheraval&a=b&foo=bar&z=anotherzval&z=zval", toTest.toQueryString());
        toTest.add("newKey", "newVal");
        assertEquals("a=anotheraval&a=b&foo=bar&newKey=newVal&z=anotherzval&z=zval", toTest.toQueryString());
        toTest.add(null, "newVal");
        assertEquals("a=anotheraval&a=b&foo=bar&newKey=newVal&z=anotherzval&z=zval", toTest.toQueryString());
        toTest.add("someKey", null);
        assertEquals("a=anotheraval&a=b&foo=bar&newKey=newVal&z=anotherzval&z=zval", toTest.toQueryString());
        toTest.add("someKey", "");
        assertEquals("a=anotheraval&a=b&foo=bar&newKey=newVal&someKey=&z=anotherzval&z=zval", toTest.toQueryString());
        toTest.add("", "someValue");
        assertEquals("=someValue&a=anotheraval&a=b&foo=bar&newKey=newVal&someKey=&z=anotherzval&z=zval",
                toTest.toQueryString());
        toTest.newUrlWithReplacedQueryString(toUrl("http://www.example.com"));
    }

    @Test
    void put() {
        var toTestFromEmpty = new QueryManipulator();
        toTestFromEmpty.put("bar", "foo");
        assertEquals("bar=foo", toTestFromEmpty.toQueryString());
        toTestFromEmpty.put("foo", "bar");
        assertEquals("bar=foo&foo=bar", toTestFromEmpty.toQueryString());
        toTestFromEmpty.put("bar", "bar");
        assertEquals("bar=bar&foo=bar", toTestFromEmpty.toQueryString());

        var toTest = new QueryManipulator("a=c&foo=bar&a=b");
        toTest.put("bar", "foo");
        assertEquals("a=b&a=c&bar=foo&foo=bar", toTest.toQueryString());
        toTest.put("foo", "nofoo");
        assertEquals("a=b&a=c&bar=foo&foo=nofoo", toTest.toQueryString());
        toTest.put("a", "d");
        assertEquals("a=d&bar=foo&foo=nofoo", toTest.toQueryString());
        toTest.put(null, "d");
        assertEquals("a=d&bar=foo&foo=nofoo", toTest.toQueryString());
        toTest.put("c", null);
        assertEquals("a=d&bar=foo&foo=nofoo", toTest.toQueryString());
        toTest.put("c", "");
        assertEquals("a=d&bar=foo&c=&foo=nofoo", toTest.toQueryString());
        toTest.put("", "nothing");
        assertEquals("=nothing&a=d&bar=foo&c=&foo=nofoo", toTest.toQueryString());

        toTest.newUrlWithReplacedQueryString(toUrl("http://www.example.com"));
    }

    @Test
    void remove() {
        var toTestFromEmpty = new QueryManipulator();
        toTestFromEmpty.remove("a key");
        assertEquals("", toTestFromEmpty.toQueryString());

        var toTest = new QueryManipulator("a=c&foo=bar&a=b");
        toTest.remove("foo");
        assertEquals("a=b&a=c", toTest.toQueryString());
        toTest.remove(null);
        assertEquals("a=b&a=c", toTest.toQueryString());
        toTest.remove("foo");
        assertEquals("a=b&a=c", toTest.toQueryString());
        toTest.remove("a");
        assertEquals("", toTest.toQueryString());

        toTest.newUrlWithReplacedQueryString(toUrl("http://www.example.com"));
    }

    @Test
    void newUrlWithReplacedQueryString() {
        var toTest = new QueryManipulator();
        var url = toUrl("http://www.example.com");
        assertEquals(url, toTest.newUrlWithReplacedQueryString(url));

        toTest = new QueryManipulator();
        url = toUrl("http://www.example.com/");
        assertEquals(url, toTest.newUrlWithReplacedQueryString(url));

        toTest = new QueryManipulator("");
        url = toUrl("http://www.example.com/");
        assertEquals(url, toTest.newUrlWithReplacedQueryString(url));

        toTest = new QueryManipulator("foo=bar&bar=foo");
        url = toUrl("http://www.example.com?bar=baz");
        assertEquals("http://www.example.com?bar=foo&foo=bar",
                toTest.newUrlWithReplacedQueryString(url).toString());

        toTest = new QueryManipulator("foo=bar&bar=foo");
        url = toUrl("http://www.example.com/this/is/a/path/?bar=baz");
        assertEquals("http://www.example.com/this/is/a/path/?bar=foo&foo=bar",
                toTest.newUrlWithReplacedQueryString(url).toString());

        toTest = new QueryManipulator("foo=bar&bar=foo");
        url = toUrl("http://jon:pass@www.example.com:7357/this/is/a/path/?bar=baz");
        assertEquals("http://jon:pass@www.example.com:7357/this/is/a/path/?bar=foo&foo=bar",
                toTest.newUrlWithReplacedQueryString(url).toString());

        toTest = new QueryManipulator("foo=bar&bar=foo");
        url = toUrl("https://jon:pass@www.example.com:7357/this/is/a/path/?bar=baz");
        assertEquals("https://jon:pass@www.example.com:7357/this/is/a/path/?bar=foo&foo=bar",
                toTest.newUrlWithReplacedQueryString(url).toString());
    }
}
