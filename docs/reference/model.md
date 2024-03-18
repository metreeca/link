---
title: Lorem Ipsum
excerpt: Lorem ipsum sit amet.
---

```typescript
type Value = null | boolean | number | string | Frame | Tagged | Typed

type Frame = Partial<{

	"{label}": Value | Value[] | Local
	"{label}={expression}": Value | Value[] | Local

	"<{expression}": Value // less than
	">{expression}": Value // greater than

	"<={expression}": Value // less than or equal (also <<)
	">={expression}": Value // greater than or equal (also >>)

	"~{expression}": string // like (stemmed word search)

	"?{expression}": Value | Value[] | Local // any

	"^{expression}": "increasing" | "decreasing" | number // order
	"${expression}": Value | Value[] | Local // focus

	"@": number // offset
	"#": number // limit

}>

type Local=Partial<{
  
	"{locale}": string | string[]
	"*": string | string[]
	"": string | string[]

}>

type Tagged = {
	"@value": string
	"@language": "{locale}"
}

type Typed = {
	"@value": string
	"@type": "{iri}"
}
```

```
iri = as defined by https://www.rfc-editor.org/rfc/rfc3987.html
```

```
locale = as defined by https://www.rfc-editor.org/rfc/rfc4646.html
```

```
expression = *(transform ":") *(["."] field)
transform = label
field = label
label = 1*("_" / DIGIT / ALPHA) / "'" *(uchar / "''") "'"
uchar = %x20-26 / %x28-10FFFF ; all non-control Unicode chars minus "'" (single quote)
```

[ABNF](https://en.wikipedia.org/wiki/Augmented_Backus–Naur_form)

# Query

```
<{expression}={value} // less than
>{expression}={value} // greater than

<%3D{expression}={value} // less than or equal
>%3D{expression}={value} // greater than or equal

<<{expression}={value} // less than or equal (alternate)
>>{expression}={value} // greater than or equal (alternate)

~{expression}={string} // like (stemmed word search)

{expression}={value} // any option
{expression}=null // non-existential any option
{expression} // existential any option

^{expression} // order
^{expression}=increasing // order
^{expression}=decreasing // order
^{expression}={number} // order

@={number} // offset
#={number} // limit
```

# Items

```json
{
  "members": [
    {
      "id": "",
      "label": "",
      ">=seniority": 3,
            "@": 100,
            "#": 10
        }
    ]
}
```

```json
{
    "members": [
        {
            "id": "/employees/123",
            "label": "Tino Faussone"
        },
        {
            "id": "/employees/098",
            "label": "Memmo Cancelli"
        }
    ]
}
```

# Stats

```json
{
    "members": [
        {
            "lower=min:seniority": 0,
            "upper=max:seniority": 0
        }
    ]
}
```

```json
{
    "members": [
        {
            "lower": 1,
            "upper": 6
        }
    ]
}
```

# Terms

```json
{
    "members": [
        {
            "~office.label": "US",
            "count=count:": 0,
            "office": {
                "id": "",
                "label": ""
            },
            "^count": -1,
          	"^office.label": 1,
            "#": 10
        }
    ]
}
```

```json
{
    "members": [
        {
            "count": 10,
            "office": {
                "id": "/offices/1",
                "label": "Paris"
            }
        },
        {
            "count": 3,
            "office": {
                "id": "/offices/1",
                "label": "Paris"
            }
        }
    ]
}
```

# Links

```json
{
    "members": [
        {
            "=count:*": "count",
            "*": {
                "id": "",
                "label": ""
            }
        }
    ]
}
```

```json
{
    "members": [
        {
            "count": 10,
            "office": {
                "id": "/offices/1",
                "label": "Paris"
            }
        },
        {
            "count": 3,
            "office": {
                "id": "/offices/1",
                "label": "Paris"
            }
        }
    ]
}
```

# Analytics

```json
{
    "members": [
        {
            "customers=count:*": 0,
            "country=customer.country": {
                "id": "",
                "label": ""
            },
            "^customers": "decreasing",
            "#": 10
        }
    ]
}
```

```json
{
    "members": [
        {
            "customers": 10,
            "country": {
                "id": "/countries/1",
                "label": "Italy"
            }
        },
        {
            "customers": 3,
            "country": {
                "id": "/countries/2",
                "label": "Germany"
            }
        }
    ]
}
```