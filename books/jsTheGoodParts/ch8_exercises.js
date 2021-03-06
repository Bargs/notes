// Ch 8 Methods


// Some helpers for later on

// A helper function for creating objects with a given prototype
if (typeof Object.create !== 'function') {
  Object.create = function (o) {
    var F = function () {};
    F.prototype = o;
    return new F();
  };
}

// replaces html special chars with their html entities
if (!String.prototype.entityify) {
    String.prototype.entityify = function () {
        return this.replace(/&/g, "&amp;").replace(/</g,
            "&lt;").replace(/>/g, "&gt;");
    };
}

// Clears all html from the browser
var clearDoc = function () {
  document.body.innerHTML = "";
}


// Lots of examples of the standard methods on the standard types

// Array

// array.concat(item...)
// Appends an item or array of items to the end of a shallow copy of the target array.
var a = ['a', 'b', 'c'];
var b = ['x', 'y', 'z'];
a.concat(b, true);


// array.join(separator)
// Creates a string from all of the array items, separated by the given separator.
var a = ['a', 'b', 'c'];
a.push('d');
a.join('');


// array.pop()
// Removes and returns the last element of the array, or undefined if it's empty.
var a = ['a', 'b', 'c'];
a.pop();
a


// array.push(item...)
// Appends items to the end of an array. Unlike concat, the original array is modified and
// array items are appended whole instead of splicing them in. Returns the new `length`.
var a = ['a', 'b', 'c'];
var b = ['x', 'y', 'z'];
a.push(b, true);
a


// array.reverse()
// Modifies the array by reversing the elements, returning the array.
var a = ['a', 'b', 'c'];
a.reverse();


// array.shift()
// Like pop but it removes the first element. Much slower than pop.
var a = ['a', 'b', 'c'];
a.shift();
a


// array.slice(start, end)
// Returns a copy of the array starting at array[start] and ending at array[end - 1].
// By default `end` is array.length if an `end` isn't provided.
var a = ['a', 'b', 'c'];
a.slice(0, 1);
a.slice(1);
a.slice(1, 2);


// array.sort(comparefn)
// Sorts an array in place. The default comparison function assumes the elements are strings, so
// it sorts numbers incorrectly. You can provide your own compareison function, which should take
// two parameters, returning 0 if they're equal, a negative number if the first is less, and a
// positive number if the second parameter is less.
var n = [4, 8, 15, 16, 23, 42];
n.sort();

n.sort(function (a, b) {
  return a - b;
});

// we can also create a function that sorts strings an numbers
var m = ['aa', 'bb', 'a', 4, 8, 15, 16, 23, 42];
m.sort(function (a, b) {
  if (a === b) {
    return 0;
  }
  if (typeof a === typeof b) {
    return a < b ? -1 : 1;
  }
  return typeof a < typeof b ? -1 : 1;
});


// array.splice(start, deleteCount, item...)
// removes `deleteCount` number of items from the array starting at `start`, replacing them with
// any `item`s passed in at the end of the args list. Unlike, `slice`, `splice` modifies the
// original array. `splice` returns the deleted elements.
var a = ['a', 'b', 'c'];
a.splice(1, 1, 'ache', 'bug');
a


// array.unshift(item...)
// Like `push` but `item`s are added to the front of the array. Returns the arrays new length.
var a = ['a', 'b', 'c'];
a.unshift('?', '@');
a



// function

// function.apply(thisarg, argArray)
// Invokes a function with and object to be bound to `this` and an optional array of args


// Number

// number.toExponential(fractionDigits)
// Converts a number to a string in exponential form. `fractionDigits` controls the number of
// decimal plcaes
var tenThousand = 10000;
tenThousand.toExponential(2);
Math.PI.toExponential(0);
Math.PI.toExponential(2);
Math.PI.toExponential(7);
Math.PI.toExponential();


// number.toFixed(fractionDigits)
// converts a number to a string in decimal form. `fractionDigits` is the number of decimals places
// default is 0, should be between 0 and 20

Math.PI.toFixed(0);
Math.PI.toFixed(2);
Math.PI.toFixed(7);
Math.PI.toFixed(16);
Math.PI.toFixed();


// number.toPrecision(precision)
// Converts number to a string in decimal form with `precision` digits of precision.
Math.PI.toPrecision(2);
Math.PI.toPrecision(7);
Math.PI.toPrecision(16);
Math.PI.toPrecision();


// number.toString(radix)
// Converts number to a string. `radix` controls the base of the number, between 2 and 36.
// Default is base 10.
Math.PI.toString(2);
Math.PI.toString(7);
Math.PI.toString(16);
Math.PI.toString();



// Object methods


// object.hasOwnProperty(name)
// Returns true if the object contains a property having the name. Prototype chain is not checked.
var a = {member: true};
var b = Object.create(a);
a.hasOwnProperty('member');
b.hasOwnProperty('member');
b.member



// RegExp methods

// regexp.exec(string)
// Most powerful and slowest of the regexp methods. Returns an array, with 0 being the
// matched text, 1 being capture group 1, 2 being group 2, etc. If the match fails null is returned.
// If the regexp has a `g` flag, searching begins at `regexp.lastIndex` instead of position 0.
// After a match `regexp.lastIndex` is set to the character after the match. An unsuccessful match
// sets `regexp.lastIndex` back to 0. This allows you to search for multiple matches by executing
// `regexp.exec(string)` in a loop.

var text = '<html><body bgcolor=linen><p>' + 'This is <b>bold<\/b>!<\/p><\/body><\/html>';
var tags = /[^<>]+|<(\/?)([A-Za-z]+)([^<>]*)>/g;
var a, i;

while ((a = tags.exec(text))) {
  for (i = 0; i < a.length; i += 1) {
    document.writeln(('// [' + i + '] ' + a[i]).entityify());
    document.writeln('<br \/>');
  }
  document.writeln('<br \/><br \/>');
}


// regexp.test(string)
// The simplest and fastest of the regexp methods. If there's a match, it returns true, otherwise
// false. Don't use the `g` flag.

var b = /&.+;/.test('frank &amp; beans');
b



// String methods

// string.charAt(pos)
// Returns the character at `pos` in the string. If `pos` < 0 or >= string.length the return is the empty string.

var name = 'Curly';
name.charAt(0);
name.charAt(-1);


// string.charCodeAt(pos)
// Like charAt but returns the interger representation of the character code
var name = 'Curly';
name.charCodeAt(0);


// String.concat(string...)
// Creates a new string by concatenating strings. Same as `+` operator for strings.
'C'.concat('a', 't');


// String.indexOf(searchString, position)
// Searches for `searchString` returning the position of the first char of the string, or -1 if not matched.
// `position` causes the search to start at that position.
var text = 'Mississippi';
text.indexOf('ss');
text.indexOf('ss', 3);
text.indexOf('ss', 6);


// string.lastIndexOf(searchString, position)
// Like indexOf but searches from the end of the string
var text = 'Mississippi';
text.lastIndexOf('ss');
text.lastIndexOf('ss', 3);
text.lastIndexOf('ss', 6);


// string.localeCompare(that)
// Compares two strings, returning -1 if `this` string is less than `that`, 0 if equal, and 1 if `this` is greater than `that`.
// The rules for the comparison are not specified.


// string.match(regexp)
// If the regexp has no `g` flag, this works like regexp.exec(string). If there is a `g` flag, it creates an array of the
// matches with the capture groups excluded.

var text = '<html><body bgcolor=linen><p>' + 'This is <b>bold<\/b>!<\/p><\/body><\/html>';
var tags = /[^<>]+|<(\/?)([A-Za-z]+)([^<>]*)>/g;
var a, i;

a = text.match(tags);
for (i = 0; i < a.length; i += 1) {
  document.writeln(('// [' + i + '] ' + a[i]).entityify());
  document.writeln('<br \/>');
}


// string.replace(searchValue, replaceValue)
// `searchValue` can be a string or a regexp object. If it's a string or a regexp without a `g` flag, only the first
// match will be replaced.

"mother_in_law".replace('_', '-');

// You must use the `g` flag to replace all occurances

"mother_in_law".replace(/_/g, '-');

// `replaceValue` can be a string or a function. `$` has special meaning when it's a string.
var oldareacode = /\((\d{3})\)/g;
'(555)666-1212'.replace(oldareacode, '$1-');

// `$$` = $
// `$&` = The matched text
// `$ number` = capture group text
// `$`` = The text preceding the match
// `$'` = The text following the match

// If `replaceValue` is a function, it is passed the matched text plus any capture groups and
// its return value is used as the replacement text.

String.prototype.entityify = function ( ) {
  var character = {
    '<' : '&lt;',
    '>' : '&gt;',
    '&' : '&amp;',
    '"' : '&quot;'
  };

  return function ( ) {
    return this.replace(/[<>&"]/g, function (c) {
      return character[c];
    });
  };
}( );

"<&>".entityify( );


// string.search(regexp)
// Like the `indexOf` method but with a regexp instead of a string. Returns the
// index of the first char of the match, or -1 if there is no match. `g` flag is ignored.


// string.slice(start, end)
// Makes a new string by copying a portion of the target string. `end` is optional. default is `string.length`.
// `end` is one greater than the position of the last character
var text = 'and in it he says "Any damn fool could';
text.slice(18);
text.slice(0, 3);
// `string.length` is added to negative numbers
text.slice(-5);
text.slice(19, 32);


// string.split(separator, limit)
// Creates an array of strings by splitting `string` into pieces. `separator` can be a string or a regexp.
// An empty string separator will create an array of single characters.
var digits = '0123456789';
digits.split('', 5);

var ip = '192.168.1.0';
ip.split('.');

// Result array will include empty strings if separator occurs at very beginning or end of string.
'|a|b|c|'.split('|');

var text = 'last, first ,middle';
text.split(/\s*,\s*/);

// Text from capturing groups will be included in the split:
text.split(/\s*(,)\s*/);


// string.substring(start, end)
// The same as slice but without adjustment for negative params. There's no reason to use substring instead of slice.

// string.toLowerCase()
// Produces a new string, converting string to lowercase

// string.toUpperCase()
// Produces a new string, converting string to uppercase

// String.fromCharCode(char...)
// Creates a string from a series of character codes
String.fromCharCode(67, 97, 116);
