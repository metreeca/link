PREFIX ec2u: <https://data.ec2u.eu/terms/>
PREFIX dct: <http://purl.org/dc/terms/>

select ?t ?u (count(?d) as ?c) {

    ?d a ec2u:Document.

    {} union { ?d dct:type/rdfs:label ?t }
    {} union { ?d ec2u:university ?u }

}

group by ?t ?u