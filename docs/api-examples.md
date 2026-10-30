# ProcureFlow API examples

Import [the Postman collection](api.postman_collection.json). Start the application
using [local setup](local-setup.md). No application login is implemented.

1. Set collection variable `runId` to a new value for each run (business codes must be unique).
2. Run Create material, List low stock, Create vendor, and Read material in order.
3. Successful creation returns 201; scripts capture returned IDs in collection variables.
   Reads return 200. Initial stock is 0 and reorder level is 10, so the new material
   appears in low-stock results.

The CI local smoke check exercises these material/vendor HTTP operations against
Compose MySQL. This collection deliberately covers that small working slice.
The README contains source-derived requisition, approval, order and receipt
requests, but their complete HTTP response serialization remains unverified:
parent/item JPA relationships can recurse. Do not treat those requests as a
verified end-to-end collection or retry a failed write blindly; a database
transaction may already have committed before JSON serialization fails.

The API has no authentication; the local profile binds to loopback. Run with
synthetic data. These requests add records; they do not clean existing data.
