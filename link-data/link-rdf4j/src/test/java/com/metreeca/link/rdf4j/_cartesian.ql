base <https://data.ec2u.eu/>

prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
prefix skos: <http://www.w3.org/2004/02/skos/core#>
prefix dct: <http://purl.org/dc/terms/>

construct where {

    </concepts/euroscivoc/45e374e6-da15-44da-9995-9ddd9b01fea9>

		rdfs:label ?cl;
		skos:prefLabel ?cp;
		skos:narrower ?n.

    ?n rdfs:label ?nl;
        skos:prefLabel ?np;
	    skos:narrower ?nn.

    ?nn rdfs:label ?nl;
        skos:prefLabel ?np.

}
