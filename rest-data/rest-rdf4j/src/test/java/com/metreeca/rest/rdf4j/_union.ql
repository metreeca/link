prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
prefix skos: <http://www.w3.org/2004/02/skos/core#>
prefix dct: <http://purl.org/dc/terms/>

# <https://data.ec2u.eu/concepts/euroscivoc/3d76a7f4-5a16-411e-ae44-7b712d5222ee> # natural sciences
# <https://data.ec2u.eu/concepts/euroscivoc/803cff73-504b-41ae-9873-1836c76c15d1> # biological sciences

construct {

    ?r ?l ?s . ?s ?p ?o

} where {

	{

		values ?s {
			<https://data.ec2u.eu/concepts/euroscivoc/803cff73-504b-41ae-9873-1836c76c15d1>
		}

		values ?p {
			rdfs:label
			skos:prefLabel
			skos:altLabel
			dct:title
		}

		?s ?p ?o

	} union {

		values ?r {
			<https://data.ec2u.eu/concepts/euroscivoc/803cff73-504b-41ae-9873-1836c76c15d1>
		}

		values ?l {
			skos:broader
		}

		?r ?l ?s

		values ?p {
			rdfs:label
			skos:prefLabel
			skos:altLabel
			dct:title
		}

		?s ?p ?o

	} union {

		{ select * {

			values ?r {
				<https://data.ec2u.eu/concepts/euroscivoc/803cff73-504b-41ae-9873-1836c76c15d1>
			}

			values ?l {
				skos:narrower
			}

			?r ?l ?s

		} limit 3 }

		values ?p {
			rdfs:label
			skos:prefLabel
			skos:altLabel
			dct:title
		}

		?s ?p ?o

	}

}
