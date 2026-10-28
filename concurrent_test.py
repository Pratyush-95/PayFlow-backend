
import json
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor

URL = "http://localhost:8080/api/payment-orders"
IDEMPOTENCY_KEY = "payflow-concurrent-test-001"

payload = json.dumps({
    "amount": 899.00,
    "currency": "INR"
}).encode("utf-8")


def send_request(number):
    request = urllib.request.Request(
        URL,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "Idempotency-Key": IDEMPOTENCY_KEY
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            body = json.loads(response.read().decode("utf-8"))
            return {
                "request": number,
                "status": response.status,
                "id": body.get("id"),
                "error": None
            }

    except urllib.error.HTTPError as error:
        return {
            "request": number,
            "status": error.code,
            "id": None,
            "error": error.read().decode("utf-8")
        }

    except Exception as error:
        return {
            "request": number,
            "status": None,
            "id": None,
            "error": str(error)
        }


if __name__ == "__main__":
    total_requests = 10

    print("Sending 10 requests concurrently...\n")

    with ThreadPoolExecutor(max_workers=10) as executor:
        results = list(
            executor.map(send_request, range(1, total_requests + 1))
        )

    for result in results:
        print(result)

    ids = {
        result["id"]
        for result in results
        if result["id"] is not None
    }

    all_successful = all(
        result["status"] in (200, 201)
        for result in results
    )

    print("\n--- SUMMARY ---")
    print("Total requests:", total_requests)
    print("Successful requests:", sum(
        result["status"] in (200, 201)
        for result in results
    ))
    print("Distinct order IDs:", len(ids))

    if all_successful and len(ids) == 1:
        print("RESULT: PASS - all requests returned the same order ID.")
    else:
        print("RESULT: CHECK FAILED - inspect the responses above.")
