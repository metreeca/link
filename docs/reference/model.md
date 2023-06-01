---
title: Lorem Ipsum
excerpt: Lorem ipsum sit amet.
---

```typescript
type Value=boolean | number | string

type Local={ [locale: string]: string } | { [locale: string]: string[] }

type Frame={

	"{field}": Value | Model | [Query]

}

type Query=Partial<Model & {

	"{field}={expression}": Value | Model | [Query]

	"<{expression}": Value // less than
	">{expression}": Value // greater than

	"<={expression}": Value // less than or equal
	">={expression}": Value // greater than or equal

	"~{expression}": string // like (stemmed word search)

	"?{expression}": (null | Value | Model)[] // any

	"^": { // order
		"{expression}": "increasing" | "decreasing"
	}

	"$": { // focus
		"{expression}": (null | Value | Model)[]
	}

	"@": number // offset
	"#": number // limit

}>

```

[ABNF](https://en.wikipedia.org/wiki/Augmented_Backus–Naur_form)

```
expression = *(transform ":") [field *(["."] field)]
transform = 1*ALPHA
field = 1*(DIGIT/ALPHA/"_") / "'" *(%[^'\\] / "\\" %['\\']) "'"; !!! review
```

Expressions may refer to one of the projected fields (`"{field}={expression}"`), but in this case references to nested
fields are not allowed.

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
            "^": {
                "count": "decreasing",
                "office.label": "increasing"
            },
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