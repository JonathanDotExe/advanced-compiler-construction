
package at.hocheneder.accproject.parser;
public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _number = 2;
	public static final int _charCon = 3;
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
		if (la.kind==n) Get(); else { SynErr(n); }
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
		while (la.kind == 4 || la.kind == 7) {
			Declaration();
		}
	}

	void Declaration() {
		if (la.kind == 4) {
			VarDecl();
		} else if (la.kind == 7) {
			FnDecl();
		} else SynErr(30);
	}

	void VarDecl() {
		Expect(4);
		Expect(1);
		Expect(5);
		Type();
		Expect(6);
	}

	void FnDecl() {
		Expect(7);
		Expect(1);
		Parameters();
		Expect(8);
		while (la.kind == 4) {
			VarDecl();
		}
		StatSeq();
		Expect(9);
	}

	void Type() {
		Expect(1);
	}

	void Parameters() {
		Expect(10);
		if (la.kind == 1) {
			Param();
			while (la.kind == 11) {
				Get();
				Param();
			}
		}
		Expect(12);
		if (la.kind == 5) {
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
		Expect(1);
		Expect(5);
		Type();
	}

	void Statement() {
		if (la.kind == 1) {
			Get();
			if (la.kind == 13) {
				Get();
				Expression();
			} else if (la.kind == 10) {
				ActParameters();
			} else SynErr(31);
			Expect(6);
		} else if (la.kind == 14) {
			Get();
			Expect(10);
			Condition();
			Expect(12);
			Expect(8);
			StatSeq();
			Expect(9);
			while (la.kind == 15) {
				Get();
				Expect(10);
				Condition();
				Expect(12);
				Expect(8);
				StatSeq();
				Expect(9);
			}
			if (la.kind == 16) {
				Get();
				Expect(8);
				StatSeq();
				Expect(9);
			}
		} else if (la.kind == 17) {
			Get();
			Expect(10);
			Condition();
			Expect(12);
			Expect(8);
			StatSeq();
			Expect(9);
		} else if (la.kind == 18) {
			Get();
			if (StartOf(2)) {
				Expression();
			}
			Expect(6);
		} else SynErr(32);
	}

	void Expression() {
		if (la.kind == 24 || la.kind == 25) {
			Addop();
		}
		Term();
		while (la.kind == 24 || la.kind == 25) {
			Addop();
			Term();
		}
	}

	void ActParameters() {
		Expect(10);
		if (StartOf(2)) {
			Expression();
			while (la.kind == 11) {
				Get();
				Expression();
			}
		}
		Expect(12);
	}

	void Condition() {
		Expression();
		Relop();
		Expression();
	}

	void Relop() {
		switch (la.kind) {
		case 13: {
			Get();
			break;
		}
		case 19: {
			Get();
			break;
		}
		case 20: {
			Get();
			break;
		}
		case 21: {
			Get();
			break;
		}
		case 22: {
			Get();
			break;
		}
		case 23: {
			Get();
			break;
		}
		default: SynErr(33); break;
		}
	}

	void Addop() {
		if (la.kind == 24) {
			Get();
		} else if (la.kind == 25) {
			Get();
		} else SynErr(34);
	}

	void Term() {
		Factor();
		while (la.kind == 26 || la.kind == 27 || la.kind == 28) {
			Mulop();
			Factor();
		}
	}

	void Factor() {
		if (la.kind == 1) {
			Get();
			if (la.kind == 10) {
				ActParameters();
			}
		} else if (la.kind == 2) {
			Get();
		} else if (la.kind == 3) {
			Get();
		} else if (la.kind == 10) {
			Get();
			Expression();
			Expect(12);
		} else SynErr(35);
	}

	void Mulop() {
		if (la.kind == 26) {
			Get();
		} else if (la.kind == 27) {
			Get();
		} else if (la.kind == 28) {
			Get();
		} else SynErr(36);
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		SimpleLang();
		Expect(0);

		scanner.buffer.Close();
	}

	private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x},
		{_x,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_x, _x,_T,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x},
		{_x,_T,_T,_T, _x,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_x,_x}

	};
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "ident expected"; break;
			case 2: s = "number expected"; break;
			case 3: s = "charCon expected"; break;
			case 4: s = "\"var\" expected"; break;
			case 5: s = "\":\" expected"; break;
			case 6: s = "\";\" expected"; break;
			case 7: s = "\"fn\" expected"; break;
			case 8: s = "\"{\" expected"; break;
			case 9: s = "\"}\" expected"; break;
			case 10: s = "\"(\" expected"; break;
			case 11: s = "\",\" expected"; break;
			case 12: s = "\")\" expected"; break;
			case 13: s = "\"=\" expected"; break;
			case 14: s = "\"if\" expected"; break;
			case 15: s = "\"elseif\" expected"; break;
			case 16: s = "\"else\" expected"; break;
			case 17: s = "\"while\" expected"; break;
			case 18: s = "\"return\" expected"; break;
			case 19: s = "\"#\" expected"; break;
			case 20: s = "\"<\" expected"; break;
			case 21: s = "\">\" expected"; break;
			case 22: s = "\">=\" expected"; break;
			case 23: s = "\"<=\" expected"; break;
			case 24: s = "\"+\" expected"; break;
			case 25: s = "\"-\" expected"; break;
			case 26: s = "\"*\" expected"; break;
			case 27: s = "\"/\" expected"; break;
			case 28: s = "\"%\" expected"; break;
			case 29: s = "??? expected"; break;
			case 30: s = "invalid Declaration"; break;
			case 31: s = "invalid Statement"; break;
			case 32: s = "invalid Statement"; break;
			case 33: s = "invalid Relop"; break;
			case 34: s = "invalid Addop"; break;
			case 35: s = "invalid Factor"; break;
			case 36: s = "invalid Mulop"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
