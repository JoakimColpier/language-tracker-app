-- Create schema (necessary in the beginning)
CREATE SCHEMA langs;

-- Create data table
CREATE TABLE langs.Dutch (
   id INT PRIMARY KEY,
   dt DATE NOT NULL,
   amount INT NOT NULL CHECK(amount>0),
   comment TEXT
   );

/* Create function add_index()
   automatically returns next id, 
   so you don't have to worry about that
*/
CREATE OR REPLACE FUNCTION langs.add_index(table_name  TEXT) RETURNS INT AS $$
DECLARE
    total INT;
BEGIN
    EXECUTE 'SELECT COUNT(*) + 1 FROM ' || table_name  INTO total;
    RETURN COALESCE(total, 1);
END;
$$ LANGUAGE plpgsql;
