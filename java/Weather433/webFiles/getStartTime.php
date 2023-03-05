<?php
// http://URL/getStartTime.php
// http://URL/getStartTime.php?version

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

	$sql = "SELECT * from HourlyTemps order by maxTime ASC limit 1";

	try {
		$conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
		$conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
		$stmt = $conn->prepare($sql); 
		$stmt->execute();
		$stmt->setFetchMode(PDO::FETCH_NUM); 
		$result = $stmt->fetchAll();
		foreach ($result as $key => $val) {
			$myJSON = json_encode($val[0]);
		}
		echo $myJSON;
	} catch(PDOException $e) {
		 echo nl2br("Error: " . $e->getMessage().PHP_EOL);
	}
	$conn = null;
}
?>