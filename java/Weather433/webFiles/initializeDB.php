<?php
// http://URL/initializeDB.php
// http://URL/initializeDB.php?version

$version = "2.8";
if (isset($_GET["version"])) {
	echo nl2br("version=".$version.PHP_EOL);
} else {
	foreach(file("hostDB.txt") as $line) {
		$tmpArray = explode('=', rtrim($line));
		if ($tmpArray[0] == "servername")
			$servername = $tmpArray[1];
		if ($tmpArray[0] == "username") 
			$username = $tmpArray[1];
		if ($tmpArray[0] == "password")
			$password = $tmpArray[1];
		if ($tmpArray[0] == "dbname") 
			$dbname = $tmpArray[1];
	}

	$sql = "CREATE TABLE IF NOT EXISTS ".$dbname.".`HourlyTemps` (
	  `maxTime` INT UNSIGNED NOT NULL,
	  `maxTemp` INT UNSIGNED NOT NULL,
	  `minTime` INT UNSIGNED NOT NULL,
	  `minTemp` INT UNSIGNED NOT NULL,
	  PRIMARY KEY (`maxTime`))";

	try {
		$conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
		$conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
		$stmt = $conn->prepare($sql); 
		$stmt->execute();
		echo nl2br($sql.PHP_EOL);
	} catch(PDOException $e) {
		 echo nl2br("Error: " . $e->getMessage().PHP_EOL);
	}
	$conn = null;
}
?>