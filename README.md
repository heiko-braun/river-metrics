Wildfly River Plugin for ElasticSearch
==================================

The Wildfly River plugin allows index wildfly metrics in ES
[Elasticsearch rivers feature](http://www.elasticsearch.org/guide/reference/river/).

In order to install the plugin, simply run: `bin/plugin -install org.wildfly/river-wildfly-metrics/1.0.0-SNAPSHOT`.

    ---------------------------------------------------
    | Wildfly Plugin          | ElasticSearch         |
    ---------------------------------------------------
    ---------------------------------------------------
    | 1.0.0-SNAPSHOT          | 0.20-0.90 -> master   |
    ---------------------------------------------------

Create river
------------

Creating the wildfly river can be done using:

```sh
curl -XPUT localhost:9200/_river/wildfly_river/_meta -d '
{
    "type" : "wildfly-metrics",
    "wildfly" : {
            "host" : "localhost",
            "port" : "9999",
            "user" : "...",
            "password" : "...",
            "schedule" : " ... (seconds)"
        }
    },
    "index" : {
        "index" : "wildfly_river",
        "type" : "wildfly"
    }
}
'
```

The above lists all the options controlling the creation of a Wildfly river.

Remove the river
================

If you need to stop the Wildfly river, you have to remove it:

```sh
curl -XDELETE http://localhost:9200/_river/wildfly_river/
```


License
-------

    This software is licensed under the Apache 2 license, quoted below.

    Copyright 2009-2013 Shay Banon and ElasticSearch <http://www.elasticsearch.org>

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
