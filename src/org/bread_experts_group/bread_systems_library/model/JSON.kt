package org.bread_experts_group.bread_systems_library.model

import java.io.EOFException
import java.io.Reader
import java.math.BigDecimal

sealed interface JSONPrimitive
value class JSONObject(val elements: Map<String, JSONPrimitive?>) : JSONPrimitive
value class JSONArray(val elements: List<JSONPrimitive?>) : JSONPrimitive
value class JSONNumber(val number: BigDecimal) : JSONPrimitive
value class JSONBoolean(val value: Boolean) : JSONPrimitive
value class JSONString(val value: String) : JSONPrimitive

fun jsonParse(input: Reader): JSONPrimitive? {
    var lastConsumed: Char? = null
    fun read(): Char {
        lastConsumed?.also {
            lastConsumed = null
            return it
        }
        val code = input.read()
        if (code == -1) throw EOFException()
        return Char(code)
    }

    fun token(): Char {
        while (true) when (val char = read()) {
            ' ', '\n', '\r', '\t' -> continue
            else -> return char
        }
    }

    fun hex(): Char {
        while (true) when (val char = read()) {
            in '0'..'9', in 'A'..'F', in 'a'..'f' -> return char
            else -> throw IllegalStateException("Expecting hexadecimal character, got '$char'")
        }
    }

    fun string(): String {
        val builder = StringBuilder()
        var escaping = false
        while (true) when (val char = read()) {
            else if escaping -> {
                when (val char = read()) {
                    '"', '\\', '/' -> builder.append(char)
                    'b' -> builder.append('\b')
                    'f' -> builder.append('\u000C')
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    't' -> builder.append('\t')
                    'u' -> builder.append(Char("${hex()}${hex()}${hex()}${hex()}".toInt(16)))
                    else -> throw IllegalStateException("Illegal escape character '$char'")
                }
                escaping = false
            }

            '\\' -> escaping = true
            '"' -> break
            else -> builder.append(char)
        }
        return builder.toString()
    }

    fun number(start: Char): BigDecimal {
        var number = BigDecimal.ZERO
        when (start) {
            '0' -> {
                when (val char = read()) {
                    '.' -> TODO("Frac")
                    'E', 'e' -> TODO("Exp")
                    ',', ' ', '\n', '\r', '\t' -> lastConsumed = char
                    else -> throw IllegalStateException("Parsing zero: got unexpected '$char'")
                }
                // fraction
                // exponent
            }

            '-' -> {
                TODO("Other")
                // whole
                // fraction
                // exponent
                number = number.negate()
            }

            else -> {
                val whole = StringBuilder(start.toString())
                while (true) when (val char = read()) {
                    '.' -> TODO("Frac")
                    'E', 'e' -> TODO("Exp")
                    ',', ' ', '\n', '\r', '\t' -> {
                        lastConsumed = char
                        break
                    }
                    in '0'..'9' -> whole.append(char)
                    else -> throw IllegalStateException("Parsing number: got unexpected '$char'")
                }
                number = number.add(BigDecimal(whole.toString()))
            }
        }
        return number
    }

    fun internalParse(): JSONPrimitive? {
        while (true) {
            when (val char = token()) {
                '{' -> {
                    val map = mutableMapOf<String, JSONPrimitive?>()
                    var expectingElement = false
                    while (true) when (val char = token()) {
                        '}' if !expectingElement -> return JSONObject(map)
                        '"' -> {
                            val key = string()
                            token().let { if (it != ':') throw IllegalStateException("Expecting ':', got '$it'") }
                            map[key] = internalParse()
                            when (token()) {
                                '}' -> return JSONObject(map)
                                ',' -> expectingElement = true
                            }
                        }

                        else -> throw IllegalStateException("Unexpected character in object '$char'")
                    }
                }

                '[' -> {
                    val list = mutableListOf<JSONPrimitive?>()
                    var expectingElement = false
                    while (true) when (val char = token()) {
                        ']' if !expectingElement -> return JSONArray(list)
                        else -> {
                            lastConsumed = char
                            list.add(internalParse())
                            when (token()) {
                                ']' -> return JSONArray(list)
                                ',' -> expectingElement = true
                            }
                        }
                    }
                }

                '"' -> return JSONString(string())
                '-', '0', in '1'..'9' -> return JSONNumber(number(char))
                't' -> {
                    read().let { if (it != 'r') throw IllegalStateException("Expecting 'r' in tRue statement, got '$it'") }
                    read().let { if (it != 'u') throw IllegalStateException("Expecting 'u' in trUe statement, got '$it'") }
                    read().let { if (it != 'e') throw IllegalStateException("Expecting 'e' in truE statement, got '$it'") }
                    return JSONBoolean(true)
                }

                'f' -> {
                    read().let { if (it != 'a') throw IllegalStateException("Expecting 'a' in fAlse statement, got '$it'") }
                    read().let { if (it != 'l') throw IllegalStateException("Expecting 'l' in faLse statement, got '$it'") }
                    read().let { if (it != 's') throw IllegalStateException("Expecting 's' in falSe statement, got '$it'") }
                    read().let { if (it != 'e') throw IllegalStateException("Expecting 'e' in falsE statement, got '$it'") }
                    return JSONBoolean(false)
                }

                'n' -> {
                    read().let { if (it != 'u') throw IllegalStateException("Expecting 'u' in nUll statement, got '$it'") }
                    read().let { if (it != 'l') throw IllegalStateException("Expecting 'l' in nuLl statement, got '$it'") }
                    read().let { if (it != 'l') throw IllegalStateException("Expecting 'l' in nulL statement, got '$it'") }
                    return null
                }

                else -> throw IllegalStateException("Unexpected character '$char'")
            }
        }
    }
    return internalParse()
}