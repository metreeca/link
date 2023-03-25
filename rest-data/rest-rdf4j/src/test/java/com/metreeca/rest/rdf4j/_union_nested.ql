base <https://data.ec2u.eu/>

prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
prefix skos: <http://www.w3.org/2004/02/skos/core#>
prefix dct: <http://purl.org/dc/terms/>

construct {

    ?c rdfs:label ?cl;
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
       skos:altLabel ?na;

       skos:narrower ?nn.

    ?nn rdfs:label ?nnl;
       dct:title ?nnt;
       skos:prefLabel ?nnp;
       skos:altLabel ?nna.

} where {

	values ?c {
		<https://data.ec2u.eu/concepts/euroscivoc/3d76a7f4-5a16-411e-ae44-7b712d5222ee>
	}

	{

		optional { ?c rdfs:label		?cl }
		optional { ?c skos:prefLabel	?cp }
		optional { ?c skos:altLabel		?ca }
		optional { ?c dct:title			?ct }

	} union {

		?c skos:broader	?b.

		optional { ?b rdfs:label		?bl }
		optional { ?b skos:prefLabel	?bp }
		optional { ?b skos:altLabel		?ba }
		optional { ?b dct:title			?bt }

	} union {

		?c skos:narrower ?n.

		optional { ?n rdfs:label		?nl }
		optional { ?n skos:prefLabel	?np }
		optional { ?n skos:altLabel		?na }
		optional { ?n dct:title			?nt }

	} union {

		?c skos:narrower ?n.
		?n skos:narrower ?nn.

		optional { ?nn rdfs:label		?nnl }
		optional { ?nn skos:prefLabel	?nnp }
		optional { ?nn skos:altLabel	?nna }
		optional { ?nn dct:title		?nnt }

	}

}
