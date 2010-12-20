/**
 * Class: ParseException
 * 
 * An exception thrown during parsing...
 *
 */

package logic.kb.fol.parser;

import java.lang.Exception;

public class ParseException
        extends Exception
{
    public int _nErrorLine;
    public String _sError;

    public ParseException(int line)
    {
	_nErrorLine = line;
	_sError     = "No additional info";
    }
    
    public ParseException(int line, String s)
    {
	_nErrorLine = line;
	_sError     = s;
    }

    public String toString() {
	return "Parse error on line " + _nErrorLine + ": " + _sError;
    }
}


