Controlled set of standard [XSD](https://www.w3.org/TR/xmlschema-2/#built-in-datatypes) data types used in UML class
definitions, internally mapped to implementation data types according to the following table.

| **UML**     | **XSD**                                                     | Java        | **Description**                                    |
| ----------- | ----------------------------------------------------------- | ----------- | -------------------------------------------------- |
| boolean     | [xsd:boolean](https://www.w3.org/TR/xmlschema-2/#boolean)   | Boolean     |                                                    |
| string      | [xsd:string](https://www.w3.org/TR/xmlschema-2/#string)     | String      |                                                    |
| integer     | [xsd:integer](https://www.w3.org/TR/xmlschema-2/#integer)   | BigInteger  | arbitrary precision integer number                 |
| decimal     | [xsd:decimal](https://www.w3.org/TR/xmlschema-2/#decimal)   | BigDecimal  | arbitrary precision decimal number                 |
| year        | [xsd:gYear](https://www.w3.org/TR/xmlschema-2/#gYear)       | Year        | absolute ISO 8601 year (yyyy)                      |
| date        | [xsd:date](https://www.w3.org/TR/xmlschema-2/#date)         |             | local ISO 8601 date (yyyy-MM-dd)                   |
| time        | [xsd:time](https://www.w3.org/TR/xmlschema-2/#time)         |             | local ISO 8601 time (hh:mm:ss.sss)                 |
| dateTime    | [xsd:dateTime](https://www.w3.org/TR/xmlschema-2/#dateTime) |             | local ISO 8601 date-time (yyyy-MM-ddThh:mm:ss.sss) |
| duration    | [xsd:duration](https://www.w3.org/TR/xmlschema-2/#duration) |             | local ISO 8601 date-time (PyYMMdDThHmMs.sS)        |
| url         | [xsd:anyURI](https://www.w3.org/TR/xmlschema-2/#anyURI)     |             | absolute URL                                       |
| text        | rdfs:langString                                             |             | human-readable localized text                      |
| enumeration |                                                             | Enumeration | closed-set values                                  |