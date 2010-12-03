(ns speclj.reporting-spec
  (:use
    [speclj.core]
    [speclj.reporting]
    [speclj.config :only (*color?*)]))

(describe "Reporting"
  (context "without color"
    (around [it] (binding [*color?* false] (it)))
    
    (it "prints all colors as plain text"
      (should= "text" (red "text"))
      (should= "text" (green "text"))
      (should= "text" (grey "text")))
    )

  (context "with color"
    (around [it] (binding [*color?* true] (it)))

    (it "prints in red"
      (should= "\u001b[31mtext\u001b[0m" (red "text")))

    (it "prints in green"
      (should= "\u001b[32mtext\u001b[0m" (green "text")))

    (it "prints in grey"
      (should= "\u001b[90mtext\u001b[0m" (grey "text")))
    ))

(run-specs)