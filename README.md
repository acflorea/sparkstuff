# sparkstuff

Apache Spark Play!ground

Test application combining:
- Apache Spark 1.5.0
- AKKA 2.3.14
- Play! Framework 2.3.8

Topics covered
- Data cleansing - uses donation.zip dataset from http://bit.ly/1Aoywaq
- Music Recommender - uses profiledata_06-May-2005.tar.gz dataset from http://bit.ly/1KiJdOR - WIP

Requires
- a runing Hadoop installation (version 2.6 was used for testing)

```
// donation.zip download
// It contains 5749142 records consisting in attempts to match hospital patients
// based on multiple criteria
$ mkdir linkage
$ cd linkage/
$ curl -o donation.zip http://bit.ly/1Aoywaq
$ unzip donation.zip
$ unzip 'block_*.zip'
// load into HDFS 
$ hadoop fs -mkdir /linkage
$ hadoop fs -put block_*.csv linkage
```
```
// profiledata_06-May-2005.tar.gz download
// It contains about 141,000 unique users, and 1.6 million unique artists. 
// About 24.2 million users’ plays of artists are recorded, along with their count.
$ mkdir ds
$ cd ds/
$ curl -o profiledata.tar.gz http://www.iro.umontreal.ca/~lisa/datasets/profiledata_06-May-2005.tar.gz
$ tar -zxvf ./profiledata.tar.gz
// load into HDFS
$ hadoop fs -mkdir /user/ds
$ hadoop fs -put ./profiledata_06-May-2005/* /user/ds
```