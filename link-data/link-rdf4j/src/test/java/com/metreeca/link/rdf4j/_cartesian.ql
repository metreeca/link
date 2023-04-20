base <https://data.ec2u.eu/>

prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
prefix skos: <http://www.w3.org/2004/02/skos/core#>
prefix dct: <http://purl.org/dc/terms/>

construct where {

    </concepts/euroscivoc/45e374e6-da15-44da-9995-9ddd9b01fea9>

		rdfs:label ?cl;
		skos:prefLabel ?cp;
		skos:altLabel ?ca;
		dct:title ?ct;

		skos:broader ?b;
		skos:narrower ?n.

    ?b rdfs:label ?bl;
       dct:title ?bt;
       skos:prefLabel ?bp;
       skos:altLabel ?ba.

    ?n rdfs:label ?nl;
       dct:title ?nt;
       skos:prefLabel ?np;
       skos:altLabel ?na.

}
