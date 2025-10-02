/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2025 Ping Identity Corporation.
 */
import { assert } from "chai";
import { isPlaceholder, containsPlaceholder, handlePlaceholder, flattenPlaceholder } from "./PlaceholderUtils";

describe("PlaceholderUtils", () => {
  describe("isPlaceholder", () => {
    describe("valid", () => {
      it("should return true when valid placeholder", () => {
        const validStringPlaceholder = "&{stub.valid.placeholder}";

        assert.isTrue(isPlaceholder(validStringPlaceholder));
      });

      it("should return true given valid placeholder containing numbers", () => {
        const validStringPlaceholder = "&{stub.vali.with9.numbers1}";

        assert.isTrue(isPlaceholder(validStringPlaceholder));
      });

      it("should return true given valid placeholder substring", () => {
        const validSubstringPlaceholder = "abcdefg 12345 ,.:/@$£'\" &{stub.valid.placeholder} abcdefg 12345 ,.:/@$£'\"";

        assert.isTrue(isPlaceholder(validSubstringPlaceholder));
      });
    });

    describe("invalid", () => {
      it("should return false given invalid placeholder", () => {
        // Note: obviously this is not a complete list of invalid placeholders - but it covers a few common typos/mistakes that could occur
        const invalidPlaceholders = [
          { placeholder: "%{invalid.placeholder" },
          { placeholder: "%invalid.placeholder}" },
          { placeholder: "${invalid.placeholder}" },
          { placeholder: "{invalid.placeholder}" },
          { placeholder: "%invalid.placeholder" },
          { placeholder: "#{invalid.placeholder}" },
          { placeholder: "&{a..b}" },
          { placeholder: "&{}" },
          { placeholder: null },
          { placeholder: undefined },
          { placeholder: 42 },
        ];

        invalidPlaceholders.forEach(({ placeholder }) => {
          const isPlaceholder = containsPlaceholder(placeholder);

          assert.isFalse(isPlaceholder);
        });
      });
    });
  });

  describe("containsPlaceholder", () => {
    describe("valid", () => {
      describe("object placeholder", () => {
        it("should return true when valid placeholder", () => {
          const placeholderObject = {
            value: "&{nested.placeholder.object}",
          };

          assert.isTrue(containsPlaceholder(placeholderObject));
        });
      });

      describe("array placeholder", () => {
        it("should return true with valid placeholder", () => {
          const placeholderStringArray = ["&{placeholder.array}"];

          assert.isTrue(containsPlaceholder(placeholderStringArray));
        });
      });
    });

    describe("invalid", () => {
      describe("object placeholder", () => {
        it("should return false when invalid placeholder", () => {
          const nestedObject = {
            nested: {
              value: "&{nested.placeholder.object}",
            },
          };

          assert.isFalse(containsPlaceholder(nestedObject));
        });
      });
      describe("array placeholder", () => {
        it("should return true with valid placeholder", () => {
          const objectArray = [{ value: "&{placeholder.array}" }];

          assert.isFalse(containsPlaceholder(objectArray));
        });
      });
    });
  });

  describe("handlePlaceholder", () => {
    it("valid placeholder", () => {
      const placeholderString = "&{stub.array.placeholder}";
      const validPlaceholders = [
        { placeholder: [placeholderString] },
        {
          placeholder: {
            value: placeholderString,
          },
        },
        { placeholder: ["non.placeholder", placeholderString, "second.non.placeholder", "third.non.placeholder"] },
      ];

      validPlaceholders.forEach(({ placeholder }) => {
        const placeholders = handlePlaceholder(placeholder);

        assert.equal(placeholders.length, 1);
        assert.isTrue(placeholders.includes(placeholderString));
      });
    });

    it("invalid placeholder", () => {
      const invalidPlaceholders = [
        { placeholder: ["non.placeholder"] },
        { placeholder: [4, 5, 6, 7] },
        {
          placeholder: {
            value: "non-placeholder",
          },
        },
      ];

      invalidPlaceholders.forEach(({ placeholder }) => {
        const placeholders = handlePlaceholder(placeholder);

        assert.equal(placeholders.length, 0);
      });
    })
  });

  describe("flattenPlaceholder", () => {
    it("should flatten nested placeholder objects", () => {
      const placeholderObjects = [
        { field: { '$bool': '&{stub.boolean.placeholder}' }, anotherField: 'stub-other-field', },
        { field: { '$list': '&{stub.list.placeholder}' }, },
        { field: { '$object': '&{stub.object.placeholder}' }, },
        { field: { '$string': '&{stub.string.placeholder}' }, },
        { field: { '$int': '&{stub.int.placeholder}' }, }
      ];

      placeholderObjects.forEach((val, index) => {
        const flattened = flattenPlaceholder(val);

        assert.equal(flattened.field, placeholderObjects[index].field);
      });
    });

    it("should return unchanged parameter object when given object is falsy", () => {
      const falsyValues = [
        null,
        undefined,
        false,
        0,
        "",
      ];

      falsyValues.forEach((val) => {
        const originalObject = flattenPlaceholder(val);

        assert.equal(val, originalObject);
      });
    });
  });
});
