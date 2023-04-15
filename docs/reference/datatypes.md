Controlled set of standard [XSD](https://www.w3.org/TR/xmlschema-2/#built-in-datatypes) data types used in UML class definitions, internally mapped to implementation data types according to the following table.

| UML         | RDF                                                          | Java           | TypeScript | JSON              | Description                                        |
| ----------- | ------------------------------------------------------------ | -------------- | ---------- | ----------------- | -------------------------------------------------- |
| boolean     | [xsd:boolean](https://www.w3.org/TR/xmlschema-2/#boolean)    | Boolean        | boolean    |                   |                                                    |
| string      | [xsd:string](https://www.w3.org/TR/xmlschema-2/#string)      | String         | string     |                   |                                                    |
| local       | [rdfs:langString](https://www.w3.org/TR/rdf-schema/#ch_langstring) |                |            |                   | localized text                                     |
| integer     | [xsd:integer](https://www.w3.org/TR/xmlschema-2/#integer)    | BigInteger     | number     | number            | arbitrary precision integer number                 |
| decimal     | [xsd:decimal](https://www.w3.org/TR/xmlschema-2/#decimal)    | BigDecimal     | number     | number            | arbitrary precision decimal number                 |
| year        | [xsd:gYear](https://www.w3.org/TR/xmlschema-2/#gYear)        | Year           | number     | “yyyy”            | absolute ISO 8601 year (yyyy)                      |
| date        | [xsd:date](https://www.w3.org/TR/xmlschema-2/#date)          | LocalDate      | Date       | “yyyy-MM-dd"      | local ISO 8601 date (yyyy-MM-dd)                   |
| time        | [xsd:time](https://www.w3.org/TR/xmlschema-2/#time)          | LocalTime      |            |                   |                                                    |
|             |                                                              | OffsetTime     |            |                   | local ISO 8601 time (hh:mm:ss.sss)                 |
| dateTime    | [xsd:dateTime](https://www.w3.org/TR/xmlschema-2/#dateTime)  | LocalDateTime  |            |                   |                                                    |
|             |                                                              | OffsetDateTime |            |                   | local ISO 8601 date-time (yyyy-MM-ddThh:mm:ss.sss) |
| instant     |                                                              |                |            |                   |                                                    |
| duration    | [xsd:duration](https://www.w3.org/TR/xmlschema-2/#duration)  |                |            |                   | local ISO 8601 duration (PyYMMdDThHmMs.sS)         |
| interval    |                                                              |                |            |                   |                                                    |
| uri         | [xsd:anyURI](https://www.w3.org/TR/xmlschema-2/#anyURI)      | URI            |            | “{uri}”           | absolute/relative URI                              |
| entry       |                                                              |                |            | { id: “{id}”, … } |                                                    |
| enumeration |                                                              |                |            |                   | closed value set                                   |