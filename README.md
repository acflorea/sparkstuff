# sparkstuff

Apache Spark Play!ground

Test application combining:
- Apache Spark 1.5.0
- AKKA 2.3.14
- Play! Framework 2.3.8

Topics covered
- Data clensing - WIP

Requires
- a runing Hadoop installation (version 2.6 was used for testing)
- donation.zip dataset from http://bit.ly/1Aoywaq
```
// download
$ mkdir linkage
$ cd linkage/
$ curl -o donation.zip http://bit.ly/1Aoywaq
$ unzip donation.zip
$ unzip 'block_*.zip'
// load into HDFS 
$ hadoop fs -mkdir linkage
$ hadoop fs -put block_*.csv linkage
```
