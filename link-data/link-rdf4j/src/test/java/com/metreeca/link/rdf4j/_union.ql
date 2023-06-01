base <https://data.ec2u.eu/>

prefix skos: <http://www.w3.org/2004/02/skos/core#>

construct {

    ?x ?xp ?xo.
    ?y ?yp ?yo.
    ?z ?zp ?zo.

} where {

	values ?x {
		<https://data.ec2u.eu/concepts/euroscivoc/3d76a7f4-5a16-411e-ae44-7b712d5222ee>
	}

	{

	    values ?xp {
            skos:prefLabel
            skos:altLabel
            skos:narrower
	    }

		?x ?xp ?xo

	} union {

        ?x skos:narrower ?y

        values ?yp {
             skos:prefLabel
             skos:altLabel
             skos:narrower
       }

       ?y ?yp ?yo

    } union {

        ?x skos:narrower/skos:narrower ?z

        values ?zp {
             skos:prefLabel
             skos:altLabel
       }

       ?z ?zp ?zo

    }

}
