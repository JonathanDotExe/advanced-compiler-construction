package at.hocheneder.accproject.parser;

import at.hocheneder.accproject.symtab.op.AddOp;
import at.hocheneder.accproject.symtab.op.MulOp;
import at.hocheneder.accproject.symtab.op.Operand;
import at.hocheneder.accproject.symtab.op.RelOp;

import static at.hocheneder.accproject.parser.Token.Kind.*;

public class Parser {
	public static final int ERROR_INVALID_DECLARATION = 30;
	public static final int ERROR_INVALID_STATEMENT_ASSIGNMENT = 31;
	public static final int ERROR_INVALID_STATEMENT = 32;
	public static final int ERROR_INVALID_ADDOP = 33;
	public static final int ERROR_INVALID_RELOP = 34;
	public static final int ERROR_INVALID_FACTOR = 35;
	public static final int ERROR_INVALID_MULOP = 36;

	public static final int _EOF = TOKEN_EOF;
	public static final int _ident = TOKEN_IDENT;
	public static final int _number = TOKEN_NUMBER;
	public static final int _charCon = TOKEN_CHARCON;

	public static final int maxT = 29;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;

	public Scanner scanner;
	public Errors errors;

	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}

	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}

	void Expect (int n) {
		if (la.kind == n) Get(); else { SynErr(n); }
	}

	boolean StartOf (int s) {
		return set[s][la.kind];
	}

	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}

	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}

	void SimpleLang() {
		Declaration();
		while (la.kind == TOKEN_VAR || la.kind == TOKEN_FN) {
			Declaration();
		}
	}

	void Declaration() {
		if (la.kind == TOKEN_VAR) {
			VarDecl();
		} else if (la.kind == TOKEN_FN) {
			FnDecl();
		} else SynErr(ERROR_INVALID_DECLARATION);
	}

	void VarDecl() {
		Expect(TOKEN_VAR);
		Expect(TOKEN_IDENT);
		Expect(TOKEN_COLON);
		Type();
		Expect(TOKEN_SEMICOLON);
	}

	void FnDecl() {
		Expect(TOKEN_FN);
		Expect(TOKEN_IDENT);
		Parameters();
		Expect(TOKEN_LBRACE);
		while (la.kind == TOKEN_VAR) {
			VarDecl();
		}
		StatSeq();
		Expect(TOKEN_RBRACE);
	}

	void Type() {
		Expect(TOKEN_IDENT);
	}

	void Parameters() {
		Expect(TOKEN_LPAREN);
		if (la.kind == TOKEN_IDENT) {
			Param();
			while (la.kind == TOKEN_COMMA) {
				Get();
				Param();
			}
		}
		Expect(TOKEN_RPAREN);
		if (la.kind == TOKEN_COLON) {
			Get();
			Type();
		}
	}

	void StatSeq() {
		Statement();
		while (StartOf(1)) {
			Statement();
		}
	}

	void Param() {
		Expect(TOKEN_IDENT);
		Expect(TOKEN_COLON);
		Type();
	}

	void Statement() {
		if (la.kind == TOKEN_IDENT) {
			Get();
			if (la.kind == TOKEN_EQUALS) {
				Get();
				Operand x = Expression();
			} else if (la.kind == TOKEN_LPAREN) {
				ActParameters();
			} else SynErr(ERROR_INVALID_STATEMENT_ASSIGNMENT);
			Expect(TOKEN_SEMICOLON);
		} else if (la.kind == TOKEN_IF) {
			Get();
			Expect(TOKEN_LPAREN);
			Operand x = Condition();
			Expect(TOKEN_RPAREN);
			Expect(TOKEN_LBRACE);
			StatSeq();
			Expect(TOKEN_RBRACE);
			while (la.kind == TOKEN_ELSEIF) {
				Get();
				Expect(TOKEN_LPAREN);
				Operand y = Condition();
				Expect(TOKEN_RPAREN);
				Expect(TOKEN_LBRACE);
				StatSeq();
				Expect(TOKEN_RBRACE);
			}
			if (la.kind == TOKEN_ELSE) {
				Get();
				Expect(TOKEN_LBRACE);
				StatSeq();
				Expect(TOKEN_RBRACE);
			}
		} else if (la.kind == TOKEN_WHILE) {
			Get();
			Expect(TOKEN_LPAREN);
			Operand x = Condition();
			Expect(TOKEN_RPAREN);
			Expect(TOKEN_LBRACE);
			StatSeq();
			Expect(TOKEN_RBRACE);
		} else if (la.kind == TOKEN_RETURN) {
			Get();
			if (StartOf(2)) {
				Operand x = Expression();
			}
			Expect(TOKEN_SEMICOLON);
		} else SynErr(ERROR_INVALID_STATEMENT);
	}

	Operand Expression() {
		Operand x;
		AddOp sign = AddOp.Plus;
		if (la.kind == TOKEN_PLUS || la.kind == TOKEN_MINUS) {
			sign = Addop();
		}
		x = Term();
		while (la.kind == TOKEN_PLUS || la.kind == TOKEN_MINUS) {
			AddOp op = Addop();
			Operand y = Term();
		}
		return x;
	}

	AddOp Addop() {
		AddOp op = null;
		if (la.kind == TOKEN_PLUS) {
			Get();
			op = AddOp.Plus;
		} else if (la.kind == TOKEN_MINUS) {
			Get();
			op = AddOp.Minus;
		} else SynErr(ERROR_INVALID_ADDOP);
		return op;
	}

	Operand Term() {
		Operand x;
		x = Factor();
		while (la.kind == TOKEN_TIMES || la.kind == TOKEN_DIVIDE || la.kind == TOKEN_MODULO) {
			MulOp op = Mulop();
			Operand y = Factor();
		}
		return x;
	}

	void ActParameters() {
		Expect(TOKEN_LPAREN);
		if (StartOf(2)) {
			Operand x = Expression();
			while (la.kind == TOKEN_COMMA) {
				Get();
				Operand y = Expression();
			}
		}
		Expect(TOKEN_RPAREN);
	}

	Operand Condition() {
		Operand x;
		x = Expression();
		RelOp op = Relop();
		Operand y = Expression();
		return x;
	}

	RelOp Relop() {
		RelOp op = null;
		switch (la.kind) {
			case TOKEN_EQUALS: {
				Get();
				op = RelOp.Equals;
				break;
			}
			case TOKEN_NOT_EQUALS: {
				Get();
				op = RelOp.NotEquals;
				break;
			}
			case TOKEN_LESS: {
				Get();
				op = RelOp.Less;
				break;
			}
			case TOKEN_GREATER: {
				Get();
				op = RelOp.Greater;
				break;
			}
			case TOKEN_GREATER_EQUALS: {
				Get();
				op = RelOp.GreaterEquals;
				break;
			}
			case TOKEN_LESS_EQUALS: {
				Get();
				op = RelOp.LessEquals;
				break;
			}
			default: SynErr(ERROR_INVALID_RELOP); break;
		}
		return op;
	}

	Operand Factor() {
		Operand x = null;
		if (la.kind == TOKEN_IDENT) {
			Get();

			if (la.kind == TOKEN_LPAREN) {
				ActParameters();
			}
		} else if (la.kind == TOKEN_NUMBER) {
			Get();
			x = new Operand(Integer.parseInt(t.val));
		} else if (la.kind == TOKEN_CHARCON) {
			Get();
			x = new Operand(t.val.charAt(0));
		} else if (la.kind == TOKEN_LPAREN) {
			Get();
			x = Expression();
			Expect(TOKEN_RPAREN);
		} else SynErr(ERROR_INVALID_FACTOR);
		return x;
	}

	MulOp Mulop() {
		MulOp op = null;
		if (la.kind == TOKEN_TIMES) {
			Get();
			op = MulOp.Times;
		} else if (la.kind == TOKEN_DIVIDE) {
			Get();
			op = MulOp.Divide;
		} else if (la.kind == TOKEN_MODULO) {
			Get();
			op = MulOp.Modulo;
		} else SynErr(ERROR_INVALID_MULOP);
		return op;
	}

	public void Parse() {
		la = new Token();
		la.val = "";
		Get();
		SimpleLang();
		Expect(TOKEN_EOF);

		scanner.buffer.Close();
	}

	private static final boolean[][] set = {
			{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x},
			{_x,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_x, _x,_T,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x},
			{_x,_T,_T,_T, _x,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_x,_x}
	};

} // end Parser


class Errors {

	public static final int ERROR_EOF_EXPECTED = 0;
	public static final int ERROR_IDENT_EXPECTED = 1;
	public static final int ERROR_NUMBER_EXPECTED = 2;
	public static final int ERROR_CHARCON_EXPECTED = 3;
	public static final int ERROR_VAR_EXPECTED = 4;
	public static final int ERROR_COLON_EXPECTED = 5;
	public static final int ERROR_SEMICOLON_EXPECTED = 6;
	public static final int ERROR_FN_EXPECTED = 7;
	public static final int ERROR_LBRACE_EXPECTED = 8;
	public static final int ERROR_RBRACE_EXPECTED = 9;
	public static final int ERROR_LPAREN_EXPECTED = 10;
	public static final int ERROR_COMMA_EXPECTED = 11;
	public static final int ERROR_RPAREN_EXPECTED = 12;
	public static final int ERROR_EQUALS_EXPECTED = 13;
	public static final int ERROR_IF_EXPECTED = 14;
	public static final int ERROR_ELSEIF_EXPECTED = 15;
	public static final int ERROR_ELSE_EXPECTED = 16;
	public static final int ERROR_WHILE_EXPECTED = 17;
	public static final int ERROR_RETURN_EXPECTED = 18;
	public static final int ERROR_NOT_EQUALS_EXPECTED = 19;
	public static final int ERROR_LESS_EXPECTED = 20;
	public static final int ERROR_GREATER_EXPECTED = 21;
	public static final int ERROR_GREATER_EQUALS_EXPECTED = 22;
	public static final int ERROR_LESS_EQUALS_EXPECTED = 23;
	public static final int ERROR_PLUS_EXPECTED = 24;
	public static final int ERROR_MINUS_EXPECTED = 25;
	public static final int ERROR_TIMES_EXPECTED = 26;
	public static final int ERROR_DIVIDE_EXPECTED = 27;
	public static final int ERROR_MODULO_EXPECTED = 28;
	public static final int ERROR_UNKNOWN_EXPECTED = 29;

	public int count = 0;
	public java.io.PrintStream errorStream = System.out;
	public String errMsgFormat = "-- line {0} col {1}: {2}";

	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) {
			b.delete(pos, pos + 3);
			b.insert(pos, line);
		}
		pos = b.indexOf("{1}");
		if (pos >= 0) {
			b.delete(pos, pos + 3);
			b.insert(pos, column);
		}
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos + 3, msg);
		errorStream.println(b.toString());
	}

	public void SynErr(int line, int col, int n) {
		String s;
		switch (n) {
			case ERROR_EOF_EXPECTED: s = "EOF expected"; break;
			case ERROR_IDENT_EXPECTED: s = "ident expected"; break;
			case ERROR_NUMBER_EXPECTED: s = "number expected"; break;
			case ERROR_CHARCON_EXPECTED: s = "charCon expected"; break;
			case ERROR_VAR_EXPECTED: s = "\"var\" expected"; break;
			case ERROR_COLON_EXPECTED: s = "\":\" expected"; break;
			case ERROR_SEMICOLON_EXPECTED: s = "\";\" expected"; break;
			case ERROR_FN_EXPECTED: s = "\"fn\" expected"; break;
			case ERROR_LBRACE_EXPECTED: s = "\"{\" expected"; break;
			case ERROR_RBRACE_EXPECTED: s = "\"}\" expected"; break;
			case ERROR_LPAREN_EXPECTED: s = "\"(\" expected"; break;
			case ERROR_COMMA_EXPECTED: s = "\",\" expected"; break;
			case ERROR_RPAREN_EXPECTED: s = "\")\" expected"; break;
			case ERROR_EQUALS_EXPECTED: s = "\"=\" expected"; break;
			case ERROR_IF_EXPECTED: s = "\"if\" expected"; break;
			case ERROR_ELSEIF_EXPECTED: s = "\"elseif\" expected"; break;
			case ERROR_ELSE_EXPECTED: s = "\"else\" expected"; break;
			case ERROR_WHILE_EXPECTED: s = "\"while\" expected"; break;
			case ERROR_RETURN_EXPECTED: s = "\"return\" expected"; break;
			case ERROR_NOT_EQUALS_EXPECTED: s = "\"#\" expected"; break;
			case ERROR_LESS_EXPECTED: s = "\"<\" expected"; break;
			case ERROR_GREATER_EXPECTED: s = "\">\" expected"; break;
			case ERROR_GREATER_EQUALS_EXPECTED: s = "\">=\" expected"; break;
			case ERROR_LESS_EQUALS_EXPECTED: s = "\"<=\" expected"; break;
			case ERROR_PLUS_EXPECTED: s = "\"+\" expected"; break;
			case ERROR_MINUS_EXPECTED: s = "\"-\" expected"; break;
			case ERROR_TIMES_EXPECTED: s = "\"*\" expected"; break;
			case ERROR_DIVIDE_EXPECTED: s = "\"/\" expected"; break;
			case ERROR_MODULO_EXPECTED: s = "\"%\" expected"; break;
			case ERROR_UNKNOWN_EXPECTED: s = "??? expected"; break;
			case Parser.ERROR_INVALID_DECLARATION: s = "invalid Declaration"; break;
			case Parser.ERROR_INVALID_STATEMENT_ASSIGNMENT: s = "invalid Statement"; break;
			case Parser.ERROR_INVALID_STATEMENT: s = "invalid Statement"; break;
			case Parser.ERROR_INVALID_ADDOP: s = "invalid Addop"; break;
			case Parser.ERROR_INVALID_RELOP: s = "invalid Relop"; break;
			case Parser.ERROR_INVALID_FACTOR: s = "invalid Factor"; break;
			case Parser.ERROR_INVALID_MULOP: s = "invalid Mulop"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr(int line, int col, String s) {
		printMsg(line, col, s);
		count++;
	}

	public void SemErr(String s) {
		errorStream.println(s);
		count++;
	}

	public void Warning(int line, int col, String s) {
		printMsg(line, col, s);
	}

	public void Warning(String s) {
		errorStream.println(s);
	}

} // Errors


class FatalError extends RuntimeException {

	public static final long serialVersionUID = 1L;

	public FatalError(String s) {
		super(s);
	}

}
