package edu.msudenver.cs.jdnss;

import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

class BindZone implements Zone {
    @Getter private String name;

    /*
    ** might not be the best to have different tables for each, but
    ** otherwise would have to find a way to differentiate the types
    ** in the single table...
    */

    private final Map<RRCode, Map<String, Vector<RR>>> tableOfTables = new Hashtable<>();

    private final Logger logger = JDNSS.logger;

    BindZone(final String name) {
        this.name = name;

        tableOfTables.put(RRCode.A, new Hashtable<>());
        tableOfTables.put(RRCode.AAAA, new Hashtable<>());
        tableOfTables.put(RRCode.NS, new Hashtable<>());
        tableOfTables.put(RRCode.MX, new Hashtable<>());
        tableOfTables.put(RRCode.CNAME, new Hashtable<>());
        tableOfTables.put(RRCode.PTR, new Hashtable<>());
        tableOfTables.put(RRCode.TXT, new Hashtable<>());
        tableOfTables.put(RRCode.HINFO, new Hashtable<>());
        tableOfTables.put(RRCode.SOA, new Hashtable<>());
        tableOfTables.put(RRCode.DNSKEY, new Hashtable<>());
        tableOfTables.put(RRCode.RRSIG, new Hashtable<>());
        tableOfTables.put(RRCode.NSEC, new Hashtable<>());
        tableOfTables.put(RRCode.NSEC3, new Hashtable<>());
        tableOfTables.put(RRCode.NSEC3PARAM, new Hashtable<>());
    }

    /**
     * Create a printable String.
     *
     * @param h a Hashtable
     * @return the contents in String form
     */
    private String dumphash(final Map<String, Vector<RR>> h) {
        String s = "";

        for (String foo : h.keySet()) {
            s += foo + ": " + h.get(foo) + " ";
        }

        return s;
    }

    /**
     * A printable version of the Zone.
     *
     * @return the string
     */
    public String toString() {
        String s = "---- Zone " + name + " -----" + '\n';

        for (RRCode i : tableOfTables.keySet()) {
            s += i.toString() + ": " + dumphash(tableOfTables.get(i)) + "\n";
        }

        s += "--------";

        return s;
    }

    private Map<String, Vector<RR>> getTable(final RRCode type) {

        final Map<String, Vector<RR>> ret = tableOfTables.get(type);

        Assertion.aver(ret != null);

        return ret;
    }

    /**
     * Add an address to a name.  There may be multiple addresses per name.
     *
     * @param name the name
     * @param rr   the resource record
     */
    public void add(final String name, final RR rr) {
        logger.traceEntry(name);
        logger.traceEntry(rr.toString());

        if ((rr instanceof SOARR) && !name.equals(this.name)) {
            this.name = name;
        }

        final Map<String, Vector<RR>> h = getTable(rr.getType());

        logger.trace(h.get(name));

        Vector<RR> value = h.get(name);

        /*
        ** if there isn't already a entry
        */
        if (value == null) {
            value = new Vector<>();
            h.put(name, value);
            value.add(rr);
        } else if (!value.contains(rr)) {
            value.add(rr);
        } else {
            logger.info(rr + " already present");
        }
    }

    /**
     * Get the Vector for a particular name.
     *
     * @param type the query type
     * @param name the name
     * @return a Vector with the appropriate addresses for the given name
     */
    public Vector<RR> get(final RRCode type, final String name) {
        final Map<String, Vector<RR>> h = getTable(type);
        Assertion.aver(h != null);
        logger.trace(h);

        final Vector<RR> v = h.get(name);
        logger.traceExit(v);

        Assertion.aver(v != null);
        return v;
    }
}
