PREFIX toys: <https://demo.metreeca.com/toys/terms#>

select ?o ?s (count(?e) as ?c) {

    ?e a toys:Employee

     { } union { ?e toys:office ?o }
     { } union { ?e toys:seniority ?s }

} group by ?o ?s