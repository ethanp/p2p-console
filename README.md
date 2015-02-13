latex input:		mmd-article-header
Title:				Simple P2P
Author:			Ethan C. Petuchowski
Base Header Level:		1
latex mode:		memoir
Keywords:			Java, Networking, P2P Systems, Peer-to-peer, BitTorrent
CSS:				http://fletcherpenney.net/css/document.css
xhtml header:		<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>
latex input:		mmd-natbib-plain
latex input:		mmd-article-begin-doc
latex footer:		mmd-memoir-footer

# P2P Console

This project is about making the simplest possible P2P network and *learning
as I go.* It is based on how BitTorrent works.

## A sample session

> NOTE: You may first have to set your router to forward ports 3000 through
> 3500 to you

Run an instance of `Console`, and in it type

    $ console
    # => <console:ipAddr>

Run a second instance of `Console`, and in it type

    $ peer
    $ set tracker <console:ipAddr>   # (from above)
    $ track asdfile

Run a third instance of `Console`, and in it type

    $ peer
    $ set tracker <console:ipAddr>   # (from above)
    $ list
    $ downoad 1

Now you'll see a directory called `downloads` into which "`asdfile`" has been
copied.

## Download algorithm

My download algorithm is downright horrible and should be changed.  I do
appreciate that it works though.


## Fun-looking next-steps

1. Performance experiments
    * Find the bottlenecks in different scenarios
        * When is the tracker-server the bottleneck?
    * TCP vs UDP
2. *DHT* -- note that this would *replace* the *"tracker"* system, making it
  fully *decentralized (!)*
    * BitTorrent uses *Mainline DHT*
        * [Wikipedia](http://en.wikipedia.org/wiki/Mainline_DHT)
        * [Protocol spec draft](http://bittorrent.org/beps/bep_0005.html)
    * Which is based on *Kademlia* (Maziéres!)
        * [Wikipedia](http://en.wikipedia.org/wiki/Kademlia)
        * [Research article](http://www.cs.rice.edu/Conferences/IPTPS02/109.pdf)
    * Makes use of *magnet links*
3. Enhance `P2PFile` to allow
    * a directory rather than just a file
    * download using multiple trackers (field `List<Tracker>` instead of
      `Tracker`)
    * Cryptographic hash values for verifying file integrity
4. Swappable algorithms, optimizations
5. Secure connection (SSL?), secure transfer (hashed chunks?), encrypted file
  chunks ("pieces")
6. Java has an API for *"watching"* a directory for changes, use that.
7. Use JDBC or something to have the Tracker class store swarms in a *"real"*
  DB rather than hash tables
8. Use Dependency Injection for downloading algorithm
9. Prefer downloading from peers who in the past have had fast download times
10. Download from peers who don't have complete copies of the file
    * This is more than halfway-implemented already
11. Add a gui interface, using JavaFX

## Glossary Mapping

Wikipedia maintains a nice [BitTorrent glossary][bt gloss], from which I
understand that

| **My Name**   | **BitTorrent Name**               |
| ------------: | :-------------------------------- |
| `Tracker`     | fills both roles tracker & index  |
| `Chunk`       | piece                             |
| `P2PFile`     | torrent                           |

## The Basic Layout

### Tracker

* Has lists of peers seeding and leeching for each file
* In the first, simplest iteration, it will just direct you to the seeders and
  initiate a download from them
    * E.g. 1/2 from one guy 1/2 from the other
* The `DHT` can come later, let's just get the files *distributed* first


[bt gloss]: http://en.wikipedia.org/wiki/Glossary_of_BitTorrent_terms

### Peer

* Has a list of files
* Can provide or obtain files from other peers

## Things left to test

1. Running the 3 consoles on 3 separate machines

## Other notes on how BitTorrent works

### Torrent File

* No actual content, just *metadata*
* Tracker, filenames, sizes, folder structure, cryptographic hashes for
  verifying file integrity (specifically SHA-1)
* Then client asks trackers about who the peers in the swarm for this file are

Format

    {
        'announce': 'trackerurl',
        'info':
        {
            'name': 'outermost-dir-name',
            'piece length': total-number-of-bytes,
            'files':
            [
                {'path': ['path/to/file.txt'], 'length': numBytes},
                {'path': ...}
            ],
            'pieces': 'cryptographic-hash-of-everything'
        }
    }

### Operation

* Segments are downloaded in a random or "rarest-first" order, ensuring high
  availability
* There are a number of *policies* for deciding how much bandwidth to use for
  each seeder/leecher pairing

## References

1. **[Computer Networks Class Project][figueroa's project]**
   -- *Javier Figueroa*
2. **Java Network Programming** -- *Elliotte Rusty Harold* (O'Reilly)
3. The [sample Chat Client][toal's tut] from Ray Toal at Loyola Marymount
   Univ.
4. When the time comes, there's a bunch of great-looking papers in the
   [*Harvard* **P2P Systems** Course][harvard p2p]
5. [Final official standard specification][spec]
6. [Longer-winded specification explanation][spec wiki]

[spec wiki]: https://wiki.theory.org/BitTorrentSpecification
[spec]:http://bittorrent.org/beps/bep_0003.html
[figueroa's project]: https://github.com/javierfigueroa/p2p-downloader
[toal's tut]: http://cs.lmu.edu/~ray/notes/javanetexamples/#chat
[harvard p2p]: http://www.eecs.harvard.edu/~mema/courses/cs264/cs264.html
